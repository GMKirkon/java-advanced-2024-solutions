package info.kgeorgiy.ja.konovalov.walk;

public class CouldNotCreateParentDirsToOutputFile extends WalkingException {
    CouldNotCreateParentDirsToOutputFile(String path) {
        super("Could not create dirs to path : " + path);
    }
}
