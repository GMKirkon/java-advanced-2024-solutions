PROJECT_ROOT="$(pwd)"

CLASS_PATH=`./getClasspaths.sh`

java -cp "$CLASS_PATH" "$PROJECT_ROOT/java-solutions/info/kgeorgiy/ja/konovalov/bank/BankTests.java"

