package info.kgeorgiy.ja.konovalov.bank.person;

import java.rmi.Remote;
import java.rmi.RemoteException;

public class RemotePerson extends AbstractPerson implements IRemotePerson {
    public RemotePerson(String name, String surname, String passportNumber) {
        super(name, surname, passportNumber);
    }
}
