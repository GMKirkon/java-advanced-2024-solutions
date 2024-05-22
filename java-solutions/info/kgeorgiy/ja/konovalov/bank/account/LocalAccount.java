package info.kgeorgiy.ja.konovalov.bank.account;

import java.rmi.RemoteException;

public class LocalAccount extends AbstractAccount implements ILocalAccount {
    
    public LocalAccount(Account other) throws RemoteException {
        super(other);
    }
    
    @Override
    public synchronized void setAmount(int amount) {
        throw new UnsupportedOperationException("Cannot do changes from local account");
    }
    
    @Override
    public synchronized void addAmount(int added) {
        throw new UnsupportedOperationException("Cannot do changes from local account");
    }
}
