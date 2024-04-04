path=..
path_to_shared_code=../../java-advanced-2024

javac -d . \
  -classpath "$path_to_shared_code/artifacts/info.kgeorgiy.java.advanced.implementor.jar:$path/java-solutions" \
  "$path"/java-solutions/info/kgeorgiy/ja/konovalov/implementor/Implementor.java

jar cfm Implementor.jar MANIFEST.MF info
rm -rf info