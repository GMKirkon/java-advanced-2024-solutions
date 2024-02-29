package info.kgeorgiy.ja.konovalov.walk;

import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1WriterAndHasher extends AbstractWriterAndHasher {
    
    final MessageDigest messageDigest;
    
    public Sha1WriterAndHasher(Writer writer) {
        super(writer);
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(String.format("Suddenly SHA-1 is not implemented by Java: %s", e.getMessage()));
        }
        
    }
    
    @Override
    public int getHashLength() {
        return messageDigest.getDigestLength() * 2;
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
}


