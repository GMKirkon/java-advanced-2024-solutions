#!/bin/bash

# Set the project root and classpath
PROJECT_ROOT="$(pwd)"
LIB_DIR="$PROJECT_ROOT/lib"
JAVA_SOLUTION_LIB_DIR="$PROJECT_ROOT/../java-advanced-2024/lib"
ARTIFACTS="$PROJECT_ROOT/../java-advanced-2024/artifacts"
OUT_DIR="$PROJECT_ROOT/out/test-classes"
SRC_DIR="$PROJECT_ROOT/java-solutions"

CLASS_PATH=`./getClasspaths.sh`

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

find java-solutions/info/kgeorgiy/ja/konovalov/bank -name "*.java" > sources.txt

# Compile the test classes
javac -classpath "$CLASS_PATH" -d "$OUT_DIR" @sources.txt

rm sources.txt

## Run the JUnit Platform Console Standalone
java -jar "$LIB_DIR/junit-platform-console-standalone-1.10.2.jar" \
     --classpath "$OUT_DIR" \
     --scan-classpath