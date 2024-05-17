#!/bin/sh

PROJECT_ROOT="$(pwd)"

JAVA_SOLUTION_LIB_DIR="$PROJECT_ROOT/../java-advanced-2024/lib"
MY_LIBS="$PROJECT_ROOT/lib"
ARTIFACTS="$PROJECT_ROOT/../java-advanced-2024/artifacts"
OUT_DIR="$PROJECT_ROOT/out/test-classes"


jar_files=$(find "$JAVA_SOLUTION_LIB_DIR" "$ARTIFACTS" "$MY_LIBS" -name "*.jar")

sep=":"
result=$(echo $jar_files | tr ' ' ':');
echo "$result"