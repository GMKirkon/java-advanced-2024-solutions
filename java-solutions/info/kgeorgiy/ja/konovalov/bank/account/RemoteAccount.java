package info.kgeorgiy.ja.konovalov.bank.account;

import info.kgeorgiy.ja.konovalov.bank.Bank;

import java.rmi.RemoteException;

public class RemoteAccount extends AbstractAccount implements IRemoteAccount {
    
    public RemoteAccount(final String id) {
        super(id);
    }
    
    public RemoteAccount(final Account other) throws RemoteException {
        super(other);
    }
}
