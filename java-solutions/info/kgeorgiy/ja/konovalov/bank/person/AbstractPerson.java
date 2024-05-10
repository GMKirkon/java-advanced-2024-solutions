package info.kgeorgiy.ja.konovalov.bank.person;

import info.kgeorgiy.ja.konovalov.bank.person.Passport;
import info.kgeorgiy.ja.konovalov.bank.person.Person;

import java.rmi.RemoteException;

public class AbstractPerson implements Person {
    
    // Yes, fields are not final. life is hard, people change their names, surnames and even passports
    private String name;
    private String surname;
    private Passport passport;
    
    public AbstractPerson(String name, String surname, Passport passport) {
        this.name = name;
        this.surname = surname;
        this.passport = passport;
    }
    
    public AbstractPerson(AbstractPerson person) {
        this.name = person.name;
        this.surname = person.surname;
        this.passport = person.passport;
    }
    
    @Override
    public String getName() throws RemoteException {
        return name;
    }
    
    public void setName(String name) throws RemoteException {
        this.name = name;
    }
    
    @Override
    public String getSurname() throws RemoteException {
        return surname;
    }
    
    public void setSurname(String surname) throws RemoteException {
        this.surname = surname;
    }
    
    @Override
    public Passport getPassport() throws RemoteException {
        return passport;
    }
    
    public void setPassport(Passport passport) throws RemoteException {
        this.passport = passport;
    }
}
