package info.kgeorgiy.ja.konovalov.bank.person;

import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.account.IRemoteAccount;
import info.kgeorgiy.ja.konovalov.bank.account.LocalAccount;
import info.kgeorgiy.ja.konovalov.bank.account.RemoteAccount;

import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson implements ILocalPerson {
    
    @Override
    public IRemoteAccount addAccount(String bankId) throws RemoteException {
        throw new UnsupportedOperationException("Cannot add accounts to local person");
    }
    
    public LocalPerson(final Person other) throws RemoteException {
        super(other.getName(), other.getSurname(), other.getPassportNumber());
        for (var entry : other.getAllAccounts().entrySet()) {
            allPersonsAccounts.put(entry.getKey(), new LocalAccount(entry.getValue()));
        }
    }
}
