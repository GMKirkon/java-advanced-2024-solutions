package info.kgeorgiy.ja.konovalov.bank.person;

import info.kgeorgiy.ja.konovalov.bank.account.RemoteAccount;

import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson implements ILocalPerson {
    public LocalPerson(String name, String surname, String passportNumber) {
        super(name, surname, passportNumber);
    }
    
    public LocalPerson(final Person other) throws RemoteException {
        super(other.getName(), other.getSurname(), other.getPassportNumber());
        for (var entry : other.getAllAccounts().entrySet()) {
            allPersonsAccounts.put(entry.getKey(), new RemoteAccount(entry.getValue()));
        }
    }
}
