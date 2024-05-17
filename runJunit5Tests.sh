#!/bin/bash

# Set the project root and classpath
PROJECT_ROOT="$(pwd)"
LIB_DIR="$PROJECT_ROOT/lib"
OUT_DIR="$PROJECT_ROOT/out/test-classes"
SRC_DIR="$PROJECT_ROOT/java-solutions"

CLASS_PATH="/Users/kirkon/Documents/java-adv/ja_idea_project/out/production/ja_idea_project:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/jsoup-1.8.1.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/quickcheck-0.6.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/opentest4j-1.3.0.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/apiguardian-api-1.1.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-jupiter-api 5.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-jupiter-api-5.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-jupiter-engine-5.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-platform-engine-1.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-platform-commons-1.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-platform-launcher-1.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-platform-suite-api 1.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-platform-standalone-1.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-platform-suite-engine 1.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/solutions/lib/junit-platform-suite-commons 1.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.base.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.walk.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.hello.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.mapper.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.crawler.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.student.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.arrayset.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.iterative.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/artifacts/info.kgeorgiy.java.advanced.implementor.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/lib/jsoup-1.8.1.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/lib/quickcheck-0.6.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/lib/opentest4j-1.3.0.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/lib/apiguardian-api-1.1.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/lib/junit-jupiter-api-5.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/lib/junit-jupiter-engine-5.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/lib/junit-platform-engine-1.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/lib/junit-platform-commons-1.10.2.jar:/Users/kirkon/Documents/java-adv/ja_idea_project/java-advanced-2024/lib/junit-platform-launcher-1.10.2.jar"

# Create the output directory if it doesn't exist
mkdir -p "$OUT_DIR"

find java-solutions/info/kgeorgiy/ja/konovalov/bank -name "*.java" > sources.txt

rm sources.txt

# Compile the test classes
javac -classpath "$CLASS_PATH" -d "$OUT_DIR" @sources.txt

# Ensure the test classes are compiled
if [ ! -d "$OUT_DIR" ]; then
    echo "Test classes directory not found. Please compile your tests first."
    exit 1
fi

# Run the JUnit Platform Console Standalone
java -jar "$LIB_DIR/junit-platform-console-standalone-1.10.2.jar" \
     --classpath "$OUT_DIR" \
     --scan-classpath