path=$(cd ..; pwd)
path_to_shared_code=$(cd ../../java-advanced-2024; pwd)

shared_implementor="$path_to_shared_code"/modules/info.kgeorgiy.java.advanced.implementor

javadoc -package -private -d "$path/javadocDirectory"\
  "$path"/java-solutions/info/kgeorgiy/ja/konovalov/implementor/*.java\
  -cp "$path/java-solutions:$shared_implementor"