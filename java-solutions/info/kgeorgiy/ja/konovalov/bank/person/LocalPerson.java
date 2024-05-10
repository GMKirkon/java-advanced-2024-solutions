package info.kgeorgiy.ja.konovalov.bank.person;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson implements Serializable {
    public LocalPerson(String name, String surname, Passport passport) {
        super(name, surname, passport);
    }
    
    /**
     * creates local version of person as copy of Remote version
     * @param person
     * @throws RemoteException
     */
    public LocalPerson(RemotePerson person) throws RemoteException {
        super(person.getName(), person.getSurname(), person.getPassport());
    }
}
