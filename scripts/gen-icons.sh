#!/usr/bin/env bash
# Regenerate all Lancar app-icon rasters from design/icon/*.svg
# Requires only: qlmanage, sips (macOS). pngcrush optional (alpha strip).
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
SRC="$ROOT/design/icon"
IOS="$ROOT/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
RES="$ROOT/composeApp/src/androidMain/res"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# render <svg> to a 1024 master png in $TMP
render_master() { # $1 svg-basename (without .svg)
  qlmanage -t -s 1024 -o "$TMP" "$SRC/$1.svg" >/dev/null 2>&1
  mv -f "$TMP/$1.svg.png" "$TMP/$1.png"
}
scale() { # $1 master-basename  $2 size  $3 out-path
  mkdir -p "$(dirname "$3")"
  sips -z "$2" "$2" "$TMP/$1.png" --out "$3" >/dev/null
}

render_master icon
render_master icon-round
render_master foreground

# iOS 1024 (opaque art; Xcode flattens alpha on build)
mkdir -p "$IOS"
cp "$TMP/icon.png" "$IOS/icon-1024.png"
command -v pngcrush >/dev/null 2>&1 && \
  pngcrush -q -rem alpha -ow "$IOS/icon-1024.png" || true

# Play store listing icon
scale icon 512 "$SRC/play-store-512.png"

# Android adaptive foreground (108dp base): mdpi..xxxhdpi
scale foreground 108 "$RES/mipmap-mdpi/ic_launcher_foreground.png"
scale foreground 162 "$RES/mipmap-hdpi/ic_launcher_foreground.png"
scale foreground 216 "$RES/mipmap-xhdpi/ic_launcher_foreground.png"
scale foreground 324 "$RES/mipmap-xxhdpi/ic_launcher_foreground.png"
scale foreground 432 "$RES/mipmap-xxxhdpi/ic_launcher_foreground.png"

# Android legacy square (48dp base)
scale icon 48  "$RES/mipmap-mdpi/ic_launcher.png"
scale icon 72  "$RES/mipmap-hdpi/ic_launcher.png"
scale icon 96  "$RES/mipmap-xhdpi/ic_launcher.png"
scale icon 144 "$RES/mipmap-xxhdpi/ic_launcher.png"
scale icon 192 "$RES/mipmap-xxxhdpi/ic_launcher.png"

# Android legacy round
scale icon-round 48  "$RES/mipmap-mdpi/ic_launcher_round.png"
scale icon-round 72  "$RES/mipmap-hdpi/ic_launcher_round.png"
scale icon-round 96  "$RES/mipmap-xhdpi/ic_launcher_round.png"
scale icon-round 144 "$RES/mipmap-xxhdpi/ic_launcher_round.png"
scale icon-round 192 "$RES/mipmap-xxxhdpi/ic_launcher_round.png"

echo "Icons regenerated."
