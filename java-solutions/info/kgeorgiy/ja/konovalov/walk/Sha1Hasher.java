package info.kgeorgiy.ja.konovalov.walk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1Hasher implements Hasher {
    final MessageDigest messageDigest;
    
    final String ERROR_HASH;
    
    {
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
            // multiply by 2 because messageDigest return number of bytes
            ERROR_HASH = "0".repeat(messageDigest.getDigestLength() * 2);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(String.format("Suddenly SHA-1 is not implemented by Java: %s", e.getMessage()));
        }
    }
    
    @Override
    public String getErrorHash() {
        return ERROR_HASH;
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
