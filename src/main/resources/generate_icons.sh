#!/bin/bash

# Check if we're in the correct directory
if [ ! -f "app_icon.svg" ]; then
    echo "Error: app_icon.svg not found in current directory"
    echo "Please run this script from the directory containing app_icon.svg"
    exit 1
fi

echo "Generating icons from app_icon.svg..."

# Generate Linux PNG (512x512)
echo "Generating PNG for Linux..."
if rsvg-convert -w 512 -h 512 app_icon.svg -o app_icon.png; then
    echo "✓ PNG icon created successfully"
else
    echo "× Error creating PNG icon"
fi

# Generate Windows ICO (multiple sizes)
echo "Generating ICO for Windows..."
for size in 16 32 48 256; do
    if ! rsvg-convert -w "$size" -h "$size" app_icon.svg -o "icon_${size}x${size}.png"; then
        echo "× Error creating icon_${size}x${size}.png"
        exit 1
    fi
done
if convert icon_16x16.png icon_32x32.png icon_48x48.png icon_256x256.png app_icon.ico; then
    echo "✓ ICO icon created successfully"
    rm icon_*.png
else
    echo "× Error creating ICO icon"
    exit 1
fi

# Generate macOS ICNS
echo "Generating ICNS for macOS..."
# Create 1024x1024 PNG
if ! rsvg-convert -w 1024 -h 1024 app_icon.svg -o icon_1024.png; then
    echo "× Error creating 1024x1024 PNG"
    exit 1
fi

# Create iconset directory
mkdir -p app_icon.iconset

# Generate all required sizes
if convert icon_1024.png -resize 16x16     app_icon.iconset/icon_16x16.png \
    && convert icon_1024.png -resize 32x32     app_icon.iconset/icon_16x16@2x.png \
    && convert icon_1024.png -resize 32x32     app_icon.iconset/icon_32x32.png \
    && convert icon_1024.png -resize 64x64     app_icon.iconset/icon_32x32@2x.png \
    && convert icon_1024.png -resize 64x64     app_icon.iconset/icon_64x64.png \
    && convert icon_1024.png -resize 128x128   app_icon.iconset/icon_64x64@2x.png \
    && convert icon_1024.png -resize 128x128   app_icon.iconset/icon_128x128.png \
    && convert icon_1024.png -resize 256x256   app_icon.iconset/icon_128x128@2x.png \
    && convert icon_1024.png -resize 256x256   app_icon.iconset/icon_256x256.png \
    && convert icon_1024.png -resize 512x512   app_icon.iconset/icon_256x256@2x.png \
    && convert icon_1024.png -resize 512x512   app_icon.iconset/icon_512x512.png \
    && cp icon_1024.png app_icon.iconset/icon_512x512@2x.png; then
    echo "✓ Iconset generated successfully"
else
    echo "× Error creating iconset"
    rm -rf app_icon.iconset icon_1024.png
    exit 1
fi

# Create ICNS file
if iconutil -c icns app_icon.iconset; then
    echo "✓ ICNS icon created successfully"
    rm -rf app_icon.iconset icon_1024.png
else
    echo "× Error creating ICNS icon"
    exit 1
fi

echo "Done! Generated icons:"
ls -l app_icon.{png,ico,icns}
