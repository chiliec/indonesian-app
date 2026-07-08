#!/usr/bin/env python3
"""
Lancar iOS-simulator driver.

Builds, installs, launches, and DRIVES the Lancar app on a booted iOS
simulator via `idb` + `xcrun simctl`. This is the programmatic handle a
future agent uses to poke the running app: dump the accessibility tree,
tap elements by label, and capture (auto-uprighted) screenshots.

Usage (run from the repo root, i.e. <unit>/):
    python3 .claude/skills/run-lancar/driver.py <command> [args]

Commands:
    boot                 Boot the target simulator (idempotent).
    build                xcodebuild the iosApp scheme for the simulator.
    install              Install the freshly built .app onto the sim.
    launch               Terminate + relaunch Lancar (foreground).
    up                   boot + build + install + launch + screenshot  (full smoke).
    describe             Dump tappable AX elements: label, app-frame, device-tap coords.
    tap "<label>"        Tap the first element whose label contains <label> (case-insensitive).
    tapxy <ax> <ay>      Tap at app-space point (ax, ay).
    screenshot [path]    Capture + auto-rotate upright. Default: /tmp/lancar.png
    orientation          Print PORTRAIT/LANDSCAPE + app + device frames.

Requires: Xcode, a booted iOS simulator, and idb
(`brew install idb-companion` + `pip3 install fb-idb`; idb CLI lives at
~/Library/Python/3.9/bin — this script adds that to PATH automatically).
"""
import json
import os
import subprocess
import sys

# --- config -----------------------------------------------------------------
SIM_DEFAULT = "057ACF07-A2C3-446D-A734-99AA3CB773AE"  # iPhone 17 Pro (see CLAUDE.md)
BUNDLE = "cx.viz.lancar"
SCHEME = "iosApp"
PROJECT = "iosApp/iosApp.xcodeproj"
JAVA_HOME = "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
IDB_BIN_DIR = os.path.expanduser("~/Library/Python/3.9/bin")

os.environ["PATH"] = IDB_BIN_DIR + ":" + os.environ.get("PATH", "")


def sim_udid():
    """Prefer an already-booted sim; fall back to the known iPhone 17 Pro."""
    out = run(["xcrun", "simctl", "list", "devices", "booted"], capture=True)
    for line in out.splitlines():
        if "Booted" in line and "(" in line:
            return line.split("(")[1].split(")")[0].strip()
    return SIM_DEFAULT


def run(cmd, capture=False, env=None, check=False):
    e = dict(os.environ)
    if env:
        e.update(env)
    r = subprocess.run(cmd, capture_output=True, text=True, env=e)
    if check and r.returncode != 0:
        sys.stderr.write(r.stdout + r.stderr)
        sys.exit(r.returncode)
    return r.stdout if capture else r.returncode


def idb_json(udid):
    """`idb ui describe-all` -> list of AX element dicts (or [])."""
    out = run(["idb", "ui", "describe-all", "--udid", udid], capture=True)
    try:
        d = json.loads(out)
        return d if isinstance(d, list) else [d]
    except Exception:
        return []


def app_frame(udid):
    for el in idb_json(udid):
        if el.get("type") == "Application":
            return el.get("frame", {}), el.get("AXLabel")
    return None, None


def device_points(udid):
    """(width_pts, height_pts) of the device in portrait — the space idb taps in."""
    out = run(["idb", "describe", "--udid", udid], capture=True)
    w = h = None
    for tok in out.replace(",", "\n").replace(")", "\n").split("\n"):
        tok = tok.strip()
        if tok.startswith("width_points="):
            w = int(float(tok.split("=")[1]))
        if tok.startswith("height_points="):
            h = int(float(tok.split("=")[1]))
    return w or 402, h or 874


def to_device(udid, ax, ay):
    """
    Map an app-space point to the device-portrait point space idb taps in.

    idb ui tap always uses the device's native PORTRAIT point space
    (e.g. 402x874). AX frames are reported in the app's CURRENT orientation.
      - Portrait  (app WxH == device WxH): identity.
      - Landscape (app is rotated): apply the rotation we verified on this
        sim -> (ax, ay) becomes (ay, W_app - ax). Only hit if the app is NOT
        portrait-locked; Lancar is locked portrait, so this is a fallback.
    """
    f, _ = app_frame(udid)
    if not f:
        return ax, ay
    wa, ha = f.get("width", 0), f.get("height", 0)
    if ha >= wa:  # portrait
        return ax, ay
    # landscape: device height (points) == app width
    _, hp = device_points(udid)
    return ay, (wa - ax)


# --- commands ----------------------------------------------------------------
def cmd_boot(udid):
    run(["xcrun", "simctl", "boot", udid])
    run(["xcrun", "simctl", "bootstatus", udid])
    run(["idb", "connect", udid])
    print("booted:", udid)


def cmd_build(udid):
    print("building (this triggers the Gradle KMP framework task; needs Java 21)...")
    rc = run([
        "xcodebuild", "-project", PROJECT, "-scheme", SCHEME,
        "-configuration", "Debug",
        "-destination", f"platform=iOS Simulator,id={udid}", "build",
    ], env={"JAVA_HOME": JAVA_HOME})
    if rc != 0:
        sys.exit("BUILD FAILED")
    print("BUILD SUCCEEDED")


def built_app_path():
    import glob
    pat = os.path.expanduser(
        "~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/"
        "Debug-iphonesimulator/iosApp.app"
    )
    apps = sorted(glob.glob(pat), key=os.path.getmtime, reverse=True)
    if not apps:
        sys.exit("no built iosApp.app found — run `build` first")
    return apps[0]


def cmd_install(udid):
    app = built_app_path()
    run(["xcrun", "simctl", "install", udid, app], check=True)
    print("installed:", app)


def cmd_launch(udid):
    run(["xcrun", "simctl", "terminate", udid, BUNDLE])
    out = run(["xcrun", "simctl", "launch", udid, BUNDLE], capture=True)
    print(out.strip() or f"launched {BUNDLE}")


def cmd_screenshot(udid, path="/tmp/lancar.png"):
    run(["xcrun", "simctl", "io", udid, "screenshot", path])
    # simctl captures the native portrait buffer; if the app is landscape the
    # content lands rotated. Upright it so the PNG reads normally.
    f, _ = app_frame(udid)
    if f and f.get("width", 0) > f.get("height", 0):
        run(["sips", "-r", "90", path])
        print("screenshot (rotated upright):", path)
    else:
        print("screenshot:", path)


def cmd_orientation(udid):
    f, label = app_frame(udid)
    dp = device_points(udid)
    if not f:
        print("no foreground app (is Lancar launched?)")
        return
    orient = "PORTRAIT" if f["height"] >= f["width"] else "LANDSCAPE"
    print(f"app={label!r} frame={f['width']:.0f}x{f['height']:.0f} -> {orient}"
          f"  | device_points={dp[0]}x{dp[1]}")


def cmd_describe(udid):
    for el in idb_json(udid):
        if el.get("type") not in ("Button", "StaticText", "Cell"):
            continue
        f = el.get("frame", {})
        cx = f.get("x", 0) + f.get("width", 0) / 2
        cy = f.get("y", 0) + f.get("height", 0) / 2
        dx, dy = to_device(udid, cx, cy)
        print(f"{el.get('type'):11} app[{f.get('x',0):.0f},{f.get('y',0):.0f} "
              f"{f.get('width',0):.0f}x{f.get('height',0):.0f}] "
              f"tap({dx:.0f},{dy:.0f})  {el.get('AXLabel')!r}")


def cmd_tap(udid, label):
    want = label.lower()
    for el in idb_json(udid):
        if el.get("type") not in ("Button", "StaticText", "Cell"):
            continue
        if want in (el.get("AXLabel") or "").lower():
            f = el["frame"]
            cx = f["x"] + f["width"] / 2
            cy = f["y"] + f["height"] / 2
            dx, dy = to_device(udid, cx, cy)
            run(["idb", "ui", "tap", str(int(dx)), str(int(dy)), "--udid", udid])
            print(f"tapped {el.get('AXLabel')!r} at device({dx:.0f},{dy:.0f})")
            return
    sys.exit(f"no element matching {label!r}")


def cmd_tapxy(udid, ax, ay):
    dx, dy = to_device(udid, float(ax), float(ay))
    run(["idb", "ui", "tap", str(int(dx)), str(int(dy)), "--udid", udid])
    print(f"tapped app({ax},{ay}) -> device({dx:.0f},{dy:.0f})")


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    cmd = sys.argv[1]
    udid = sim_udid()
    if cmd == "boot":
        cmd_boot(udid)
    elif cmd == "build":
        cmd_build(udid)
    elif cmd == "install":
        cmd_install(udid)
    elif cmd == "launch":
        cmd_launch(udid)
    elif cmd == "up":
        cmd_boot(udid); cmd_build(udid); cmd_install(udid); cmd_launch(udid)
        import time
        time.sleep(3)
        cmd_screenshot(udid)
    elif cmd == "describe":
        cmd_describe(udid)
    elif cmd == "orientation":
        cmd_orientation(udid)
    elif cmd == "tap":
        cmd_tap(udid, sys.argv[2])
    elif cmd == "tapxy":
        cmd_tapxy(udid, sys.argv[2], sys.argv[3])
    elif cmd == "screenshot":
        cmd_screenshot(udid, sys.argv[2] if len(sys.argv) > 2 else "/tmp/lancar.png")
    else:
        sys.exit(f"unknown command: {cmd}")


if __name__ == "__main__":
    main()
