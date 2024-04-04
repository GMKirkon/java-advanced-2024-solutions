path=..
path_to_shared_code=../../java-advanced-2024
shared_implementor_module="$path_to_shared_code"/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/

javadoc -package -private -d "$path/javadoc"\
  -classpath "$path_to_shared_code/modules:$path/java-solutions" \
  "$shared_implementor_module"/Impler.java \
  "$shared_implementor_module"/JarImpler.java \
  "$shared_implementor_module"/ImplerException.java \
  "$path"/java-solutions/info/kgeorgiy/ja/konovalov/implementor/*.java
