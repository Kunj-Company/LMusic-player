#!/usr/bin/env python3
"""LMusic emulator smoke test: launches the app, walks through the main screens with
uiautomator, takes screenshots and fails if the process dies or a FATAL EXCEPTION appears."""
import re, subprocess, sys, time, os, xml.etree.ElementTree as ET

PKG = "com.twilighty.lmusic"
ACT = f"{PKG}/com.twilighty.lmusic.MainActivity"
OUT = "launch-logs"
os.makedirs(OUT, exist_ok=True)
steps, problems = [], []

def adb(*args, check=False, capture=True):
    r = subprocess.run(["adb", *args], capture_output=capture, text=True)
    return r.stdout if capture else ""

def alive():
    return adb("shell", "pidof", PKG).strip() != ""

def shot(name):
    with open(f"{OUT}/{name}.png", "wb") as f:
        f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout)

def dump():
    out = adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    if "dumped" not in out:
        print("uiautomator:", out.strip()[:200])
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    try:
        return ET.fromstring(xml[xml.find("<"):])
    except Exception:
        return None

def find(label):
    root = dump()
    if root is None:
        return None
    for node in root.iter("node"):
        for attr in ("text", "content-desc"):
            v = (node.get(attr) or "").strip()
            if v.lower() == label.lower():
                m = re.findall(r"\d+", node.get("bounds", ""))
                if len(m) == 4:
                    x1, y1, x2, y2 = map(int, m)
                    return ((x1 + x2) // 2, (y1 + y2) // 2)
    return None

def texts():
    root = dump()
    if root is None:
        return []
    out = []
    for node in root.iter("node"):
        for attr in ("text", "content-desc"):
            v = (node.get(attr) or "").strip()
            if v:
                out.append(v)
    return out

def has_text(fragment, tries=1, wait=3):
    for i in range(tries):
        if any(fragment.lower() in t.lower() for t in texts()):
            return True
        if i < tries - 1:
            time.sleep(wait)
    return False

def expect(name, ok):
    print(f"[{'PASS' if ok else 'FAIL'}] check: {name}")
    if not ok:
        problems.append(f"check failed: {name}")

def size():
    m = re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
    return (int(m.group(1)), int(m.group(2))) if m else (1080, 2400)

W, H = 0, 0

def swipe_up():
    adb("shell", "input", "swipe", str(W // 2), str(int(H * 0.75)), str(W // 2), str(int(H * 0.35)), "350")
    time.sleep(1)

def tap(label, scroll=0):
    for i in range(scroll + 1):
        p = find(label)
        if p:
            adb("shell", "input", "tap", str(p[0]), str(p[1]))
            time.sleep(2.5)
            return True
        if i < scroll:
            swipe_up()
    return False

def step(name, fn):
    ok = False
    try:
        ok = fn()
    except Exception as e:
        print(f"step {name} error: {e}")
    time.sleep(1)
    shot(name)
    a = alive()
    steps.append((name, ok, a))
    print(f"[{'ok' if ok else 'skip'}] {name} — process {'alive' if a else 'DEAD'}")
    if not a:
        problems.append(f"process died after step {name}")
        raise SystemExit
    return ok

def back():
    adb("shell", "input", "keyevent", "KEYCODE_BACK"); time.sleep(1.5)

def launch(*extra):
    adb("shell", "am", "start", "-W", "-n", ACT, "--ez", "is_benchmark", "true", *extra)
    time.sleep(6)

def tap_xy(fx, fy):
    adb("shell", "input", "tap", str(int(W * fx)), str(int(H * fy)))
    time.sleep(2.5)
    return True

def main():
    global W, H
    W, H = size()
    adb("logcat", "-c"); adb("logcat", "-b", "crash", "-c")
    # 1) First launch: setup / welcome screen.
    adb("shell", "am", "start", "-W", "-n", ACT); time.sleep(8)
    step("01-first-launch-setup", lambda: True)
    # 2) Skip setup (benchmark flag) and walk the app.
    adb("shell", "am", "force-stop", PKG); time.sleep(1)
    launch(); time.sleep(4)
    step("02-home", lambda: True)
    # Shuffle-all syncs the library when it is empty, so the test songs get indexed.
    adb("shell", "am", "start", "-W", "-a", "com.twilighty.lmusic.ACTION_SHUFFLE_ALL", "-n", ACT, "--ez", "is_benchmark", "true")
    time.sleep(12)
    tap("Library") or tap_xy(0.4, 0.94)
    expect("test songs appear in Library", has_text("LMusic Test", tries=10, wait=4))
    shot("02b-library-with-songs")
    tap("Home") or tap_xy(0.2, 0.94)
    # Bottom nav: tap by label when the accessibility tree is readable, else by position.
    for k, tab in enumerate(["Library", "Search", "Stats", "Home"]):
        x = [0.4, 0.6, 0.8, 0.2][k]
        step(f"03-tab-{tab.lower()}", lambda t=tab, x=x: tap(t) or tap_xy(x, 0.905))
    step("04-swipe-to-library", lambda: (adb("shell", "input", "swipe", str(int(W*0.85)), str(int(H*0.3)), str(int(W*0.1)), str(int(H*0.3)), "180"), True)[1])
    expect("swipe left on Home opens Library", has_text("SONGS", tries=2) and not has_text("Search…") and not has_text("Search..."))
    step("04b-swipe-back-home", lambda: (adb("shell", "input", "swipe", str(int(W*0.1)), str(int(H*0.3)), str(int(W*0.85)), str(int(H*0.3)), "180"), True)[1])
    expect("swipe right on Library returns Home", has_text("Listen", tries=2))
    # Screens opened directly (benchmark-only "smoke_route" extra handled in MainActivity).
    for route in ["lofi", "focus", "garden", "party", "taste_match", "cover_studio", "settings",
                  "smart_features_settings", "library_health", "recap/week"]:
        step(f"05-route-{route.replace('/', '-')}", lambda r=route: (launch("--es", "smoke_route", r), True)[1])
        back()
    # Playback: shuffle all (app shortcut action), then the mini player and the full player.
    def shuffle_all():
        adb("shell", "am", "start", "-W", "-a", "com.twilighty.lmusic.ACTION_SHUFFLE_ALL", "-n", ACT, "--ez", "is_benchmark", "true")
        time.sleep(8); return True
    step("06-shuffle-all", shuffle_all)
    expect("a test song is playing (mini player)", has_text("LMusic Test", tries=4))
    before = sorted(t for t in texts() if t.startswith("Test Artist") or t.startswith("LMusic Test"))
    step("07-mini-player-swipe-next", lambda: (adb("shell", "input", "swipe", str(int(W*0.8)), str(int(H*0.82)), str(int(W*0.2)), str(int(H*0.82)), "200"), True)[1])
    time.sleep(2)
    after = sorted(t for t in texts() if t.startswith("Test Artist") or t.startswith("LMusic Test"))
    print("mini player before:", before, "after:", after)
    expect("mini player swipe changes the song", bool(before) and bool(after) and before != after)
    step("08-open-player", lambda: (adb("shell", "am", "start", "-W", "-n", ACT, "--ez", "is_benchmark", "true", "--ez", "ACTION_SHOW_PLAYER", "true"), time.sleep(5), True)[2])
    expect("now playing shows the song", has_text("Test Artist", tries=3))
    step("09-now-playing-double-tap", lambda: (tap_xy(0.5, 0.33), tap_xy(0.5, 0.33), True)[2])
    back()

try:
    main()
finally:
    crash = adb("logcat", "-d", "-b", "crash")
    full = adb("logcat", "-d")
    print("===== swipe log =====")
    print("\n".join(l for l in full.splitlines() if "LMusicSwipe" in l))
    open(f"{OUT}/crash.txt", "w").write(crash)
    open(f"{OUT}/logcat.txt", "w").write(full)
    ours = f"Process: {PKG}," in crash or f"Process: {PKG}," in full
    if ours:
        problems.append("FATAL EXCEPTION in logcat")
        print("===== crash buffer =====\n" + crash)
        idx = full.find("FATAL EXCEPTION")
        print(full[idx: idx + 6000])
    print("\nSUMMARY")
    for n, ok, a in steps:
        print(f"  {n}: {'ok' if ok else 'not found'}, {'alive' if a else 'DEAD'}")
    if problems:
        print("PROBLEMS: " + "; ".join(problems))
        sys.exit(1)
    print("SMOKE TEST PASSED")
