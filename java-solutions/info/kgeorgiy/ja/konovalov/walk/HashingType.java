package info.kgeorgiy.ja.konovalov.walk;

import java.io.Writer;
import java.security.NoSuchAlgorithmException;

public enum HashingType {
    SHA_1 {
        @Override
        public AbstractWriterAndHasher makeHasher(Writer writer){
            return new Sha1WriterAndHasher(writer);
        }
    },
    JENKINS {
        @Override
        public AbstractWriterAndHasher makeHasher(Writer writer) {
            return new JenkingWriterAndHasher(writer);
        }
    };
    
    public abstract AbstractWriterAndHasher makeHasher(Writer writer);
}
