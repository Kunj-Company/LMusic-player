package com.twilighty.lmusic.data.smart

import android.content.Context
import com.twilighty.lmusic.data.database.SmartSongRow
import com.twilighty.lmusic.data.database.SongFeaturesEntity
import java.io.Closeable
import timber.log.Timber

/**
 * Analyses one song at a time: decode 3 short windows → YAMNet scores + DSP → tags.
 * Blocking; run on a background dispatcher. Never throws — failures become STATUS_FAILED rows.
 */
class SongAnalyzer(context: Context) : Closeable {

    private val decoder = AudioWindowDecoder(context)
    private val classifier = YamnetClassifier(context)

    fun analyze(song: SmartSongRow, isCancelled: () -> Boolean): SongFeaturesEntity? {
        val songId = song.id.toString()
        return try {
            val source = song.contentUriString.ifBlank { song.filePath }
            val windows = decoder.decodeWindows(source, song.duration, isCancelled = isCancelled)
                .filter { AudioDsp.meanAbs(it) > 0.0005f }
            if (isCancelled()) return null
            if (windows.isEmpty()) return failed(songId)
            val scores = classifier.classifyWindows(windows, isCancelled) ?: return if (isCancelled()) null else failed(songId)
            val dsp = AudioDsp.compute(windows)
            val moods = SmartTagger.moodTags(scores, dsp.rmsMean, dsp.tempoBpm, dsp.zcr)
            val genres = SmartTagger.genreTags(scores)
            SongFeaturesEntity(
                songId = songId,
                modelVersion = SmartModel.MODEL_VERSION,
                analyzedAt = System.currentTimeMillis(),
                scores = SmartVectors.quantize(scores),
                energy = dsp.rmsMean,
                energyVar = dsp.rmsVar,
                tempo = dsp.tempoBpm,
                brightness = dsp.zcr,
                moodTags = SmartTagger.joinTags(moods),
                genreTags = SmartTagger.joinTags(genres),
                status = SongFeaturesEntity.STATUS_OK
            )
        } catch (t: Throwable) {
            if (t is OutOfMemoryError) System.gc()
            Timber.w(t, "Smart: analysis failed for song %s", songId)
            failed(songId)
        }
    }

    private fun failed(songId: String) = SongFeaturesEntity(
        songId = songId,
        modelVersion = SmartModel.MODEL_VERSION,
        analyzedAt = System.currentTimeMillis(),
        scores = null,
        energy = 0f,
        energyVar = 0f,
        tempo = 0f,
        brightness = 0f,
        moodTags = "",
        genreTags = "",
        status = SongFeaturesEntity.STATUS_FAILED
    )

    override fun close() {
        classifier.close()
    }
}
