package info.kgeorgiy.ja.konovalov.bank;

public abstract class AbstractSinglePolicyRmiScheduler implements RmiRegistriesScheduler {
    
    public abstract int getPort();
    
    @Override
    public int getAccountPort() {
        return getPort();
    }
    
    @Override
    public int getPersonPort() {
        return getPort();
    }
}
