package info.kgeorgiy.ja.konovalov.walk;

import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1WalkWriterAndHasher extends AbstractWalkWriterAndHasher {
    
    final MessageDigest messageDigest;
    
    public Sha1WalkWriterAndHasher(Writer writer) throws NoSuchAlgorithmException {
        super(writer);
        messageDigest = MessageDigest.getInstance("SHA-1");
    }
    
    @Override
    public byte[] getHash() {
        return messageDigest.digest();
    }
    
    @Override
    public void hash(byte[] data, int len) {
        messageDigest.update(data, 0, len);
    }
    
    @Override
    public void reset() {
        messageDigest.reset();
    }
    
    
    @Override
    public void writeZeroHash(String file) throws java.io.IOException {
        super.writer.write(String.format("%040x ", 0) + file + System.lineSeparator());
    }
}


