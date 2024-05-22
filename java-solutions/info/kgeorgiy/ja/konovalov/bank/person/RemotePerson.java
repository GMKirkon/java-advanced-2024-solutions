package info.kgeorgiy.ja.konovalov.bank.person;

import info.kgeorgiy.ja.konovalov.bank.Bank;
import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.account.IRemoteAccount;

import java.rmi.RemoteException;
import java.util.Objects;

public class RemotePerson extends AbstractPerson implements IRemotePerson {
    private final Bank bank;
    
    public RemotePerson(String name, String surname, String passportNumber, Bank bank) {
        super(name, surname, passportNumber);
        this.bank = bank;
    }
    
    @Override
    public IRemoteAccount addAccount(String bankId) throws RemoteException {
        synchronized (bank) {
            IRemoteAccount account = bank.createAccountForPerson(bankId, this);
            String fullId = getAccountActualId(bankId);
            allPersonsAccounts.put(fullId, account);
            return account;
        }
    }
}
