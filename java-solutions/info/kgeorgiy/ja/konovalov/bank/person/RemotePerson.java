package info.kgeorgiy.ja.konovalov.bank.person;

public class RemotePerson extends AbstractPerson implements IRemotePerson {
    public RemotePerson(String name, String surname, String passportNumber) {
        super(name, surname, passportNumber);
    }
}
