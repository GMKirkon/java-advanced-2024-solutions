package info.kgeorgiy.ja.konovalov.bank.person;

import info.kgeorgiy.ja.konovalov.bank.person.AbstractPerson;
import info.kgeorgiy.ja.konovalov.bank.person.Passport;

public class RemotePerson extends AbstractPerson {
    public RemotePerson(String name, String surname, Passport passport) {
        super(name, surname, passport);
    }
}
