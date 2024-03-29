path=$(cd ..; pwd)
path_to_shared_code=$(cd ../../shared; pwd)

shared_implementor=$path_to_shared_code/modules/info.kgeorgiy.java.advanced.implementor

javac -d "$(pwd)" \
  "$path"/java-solutions/info/kgeorgiy/ja/konovalov/implementor/Implementor.java\
  -cp "$path/java-solutions:$shared_implementor"

jar cfm ../buildDirectory/Implementor.jar MANIFEST.MF info
rm -r info