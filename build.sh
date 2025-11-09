#!/bin/bash
set -e

echo "Cleaning up class files..."
find build -name "*.class" -type f -delete 2>/dev/null || true
echo "Cleaned up class files."

echo "Adding standard library..."
mkdir -p build
cp -f RainLang/standard_lib.txt build/
echo "Standard library added."

echo "Compiling solution..."
javac -d build src/*.java
echo "Build complete."
