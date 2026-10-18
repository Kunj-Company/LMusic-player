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
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
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

def main():
    global W, H
    W, H = size()
    adb("logcat", "-c"); adb("logcat", "-b", "crash", "-c")
    # 1) First launch: setup / welcome screen.
    adb("shell", "am", "start", "-W", "-n", ACT); time.sleep(8)
    step("01-first-launch-setup", lambda: True)
    # 2) Skip setup (benchmark flag) and walk the app.
    adb("shell", "am", "force-stop", PKG); time.sleep(1)
    adb("shell", "am", "start", "-W", "-n", ACT, "--ez", "is_benchmark", "true"); time.sleep(10)
    step("02-home", lambda: True)
    for tab in ["Library", "Search", "Stats", "Home"]:
        step(f"03-tab-{tab.lower()}", lambda t=tab: tap(t))
    # Swipe between pages.
    step("04-swipe-to-library", lambda: (adb("shell", "input", "swipe", str(int(W*0.85)), str(H//2), str(int(W*0.1)), str(H//2), "200"), True)[1])
    step("04b-back-home", lambda: tap("Home"))
    # Spaces.
    for label, name in [("Lofi", "lofi"), ("Focus", "focus"), ("Garden", "garden"), ("Party", "party"),
                        ("Taste match", "taste"), ("Cover studio", "covers")]:
        if step(f"05-space-{name}", lambda l=label: tap(l, scroll=3)):
            back()
        tap("Home")
    step("06-settings", lambda: tap("Settings"))
    back()
    # Play a song from Library if there is one.
    tap("Library")
    step("07-library-songs", lambda: tap("Songs") or True)
    def play_first():
        root = dump()
        if root is None: return False
        for node in root.iter("node"):
            if (node.get("text") or "").startswith("LMusic Test"):
                m = list(map(int, re.findall(r"\d+", node.get("bounds"))))
                adb("shell", "input", "tap", str((m[0]+m[2])//2), str((m[1]+m[3])//2)); time.sleep(4)
                return True
        return False
    if step("08-play-song", play_first):
        # Open the full player by tapping the mini player area near the bottom.
        step("09-mini-player-swipe-next", lambda: (adb("shell", "input", "swipe", str(int(W*0.8)), str(int(H*0.86)), str(int(W*0.2)), str(int(H*0.86)), "200"), True)[1])
        step("10-open-player", lambda: (adb("shell", "input", "tap", str(W//2), str(int(H*0.86))), True)[1])
        time.sleep(3)
        step("11-now-playing", lambda: True)
        back()

try:
    main()
finally:
    crash = adb("logcat", "-d", "-b", "crash")
    full = adb("logcat", "-d")
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
