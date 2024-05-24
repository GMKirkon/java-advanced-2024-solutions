package info.kgeorgiy.ja.konovalov.bank.person;

import info.kgeorgiy.ja.konovalov.bank.account.Account;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractPerson implements Person {
    protected final String name;
    protected final String surname;
    protected final String passportNumber;
    protected final ConcurrentHashMap<String, Account> allPersonsAccounts = new ConcurrentHashMap<>();

    public AbstractPerson(final String name, final String surname, final String passportNumber) {
        this.name = name;
        this.surname = surname;
        this.passportNumber = passportNumber;
    }

    @Override
    public Map<String, Account> getAllAccounts() {
        return allPersonsAccounts;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public String getPassportNumber() {
        return passportNumber;
    }

    @Override
    public Account getAccount(String accountBankId) {
        return allPersonsAccounts.get(getAccountActualId(accountBankId));
    }

    protected String getAccountActualId(String subId) {
        return String.format("%s:%s", getPassportNumber(), subId);
    }
}
