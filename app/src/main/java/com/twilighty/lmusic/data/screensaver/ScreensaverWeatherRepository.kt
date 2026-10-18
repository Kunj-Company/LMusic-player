package com.twilighty.lmusic.data.screensaver

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Weather for the screensaver from Open-Meteo (free, no key). Only called while the screensaver is
 * showing and "Show weather" is on. Cached for 30 min; every failure returns null (weather hidden).
 */
@Singleton
class ScreensaverWeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: ScreensaverPreferences,
) {

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Current weather, or null when off / no location / offline. Never throws. */
    suspend fun currentWeather(settings: ScreensaverSettings): WeatherSnapshot? = runCatching {
        if (!settings.showWeather) return null
        val target = resolveLocation(settings.location) ?: return null
        val now = System.currentTimeMillis()
        val cached = preferences.cachedWeather()
        if (OpenMeteo.isFresh(cached, target.lat, target.lon, now)) {
            return cached?.copy(city = target.city ?: cached.city)
        }
        val body = httpGet(OpenMeteo.forecastUrl(target.lat, target.lon)) ?: return cached?.takeIf { now - it.fetchedAtMs < 3 * OpenMeteo.CACHE_MS }
        val snapshot = OpenMeteo.parseForecast(body, target.city, target.lat, target.lon, now) ?: return null
        preferences.saveWeather(snapshot)
        snapshot
    }.getOrNull()

    suspend fun searchCity(name: String): List<GeoPlace> {
        if (name.isBlank()) return emptyList()
        val body = httpGet(OpenMeteo.geocodingUrl(name)) ?: return emptyList()
        return OpenMeteo.parseGeocoding(body)
    }

    private data class Target(val lat: Double, val lon: Double, val city: String?)

    private suspend fun resolveLocation(setting: WeatherLocationSetting): Target? {
        if (setting.useDeviceLocation && hasLocationPermission()) {
            val loc = lastKnownLocation()
            if (loc != null) return Target(loc.latitude, loc.longitude, cityName(loc))
        }
        val lat = setting.cityLat
        val lon = setting.cityLon
        return if (lat != null && lon != null) Target(lat, lon, setting.cityName) else null
    }

    @Suppress("MissingPermission")
    private fun lastKnownLocation(): Location? = runCatching {
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        listOf(LocationManager.NETWORK_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }.getOrNull()

    private suspend fun cityName(loc: Location): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        withTimeoutOrNull(4_000L) {
            suspendCancellableCoroutine { cont ->
                Geocoder(context, Locale.getDefault()).getFromLocation(
                    loc.latitude, loc.longitude, 1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            val a = addresses.firstOrNull()
                            if (cont.isActive) cont.resume(a?.locality ?: a?.subAdminArea ?: a?.adminArea)
                        }
                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            }
        }
    }.getOrNull()

    private suspend fun httpGet(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (conn.responseCode !in 200..299) null
                else conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }
}
