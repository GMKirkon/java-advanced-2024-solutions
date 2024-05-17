package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.account.RemoteAccount;
import info.kgeorgiy.ja.konovalov.bank.person.IRemotePerson;
import info.kgeorgiy.ja.konovalov.bank.person.LocalPerson;
import info.kgeorgiy.ja.konovalov.bank.person.Person;
import info.kgeorgiy.ja.konovalov.bank.person.RemotePerson;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RemotePerson> remotePersons = new ConcurrentHashMap<>();
    private final RmiRegistriesScheduler scheduler;
    
    public RemoteBank(final int rmiRegistryExportingPort) {
        scheduler = RmiAccountsPolicy.SINGLE.getScheduler(List.of(rmiRegistryExportingPort));
    }
    
    public RemoteBank(List<Integer> ports, RmiAccountsPolicy policy) {
        scheduler = policy.getScheduler(ports);
    }
    
    @Override
    public Account createAccount(final String id) throws RemoteException {
        System.out.printf("Creating account %s%n", id);
        if (accounts.get(id) != null) {
            throw new IllegalArgumentException(String.format("account with provided id:%s is already created", id));
        }
        
        final Account account = new RemoteAccount(id);
        if (accounts.putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, scheduler.getAccountPort());
            return account;
        } else {
            return getAccount(id);
        }
    }
    
    @Override
    public Account createAccountForPerson(final String id, final Person person) throws RemoteException {
        String fullId = String.format("%s:%s", person.getPassportNumber(), id);
        var account = createAccount(fullId);
        person.addAccount(account, id);
        return account;
    }
    
    @Override
    public synchronized RemotePerson createPerson(String name, String surname, String passportNumber) throws RemoteException {
        System.out.printf("Creating person %s %s %s%n", name, surname, passportNumber);
        
        if (remotePersons.get(passportNumber) != null) {
            throw new IllegalArgumentException("Person with such passport is already registered");
        }
        
        int port = scheduler.getPersonPort();
        var person = new RemotePerson(name, surname, passportNumber);
        if (remotePersons.putIfAbsent(passportNumber, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
        }
        return person;
    }
    
    @Override
    public Account getAccount(final String id) {
        System.out.printf("Retrieving account with id: %s%n", id);
        return accounts.get(id);
    }
    
    @Override
    public LocalPerson getLocalPerson(final String passportNumber) throws RemoteException {
        System.out.printf("obtaining local person with passport: %s%n", passportNumber);
        var remotePerson = getRemotePerson(passportNumber);
        if (remotePerson != null) {
            return new LocalPerson(getRemotePerson(passportNumber));
        } else {
            return null;
        }
    }
    
    @Override
    public RemotePerson getRemotePerson(final String passportNumber) throws RemoteException {
        System.out.printf("obtaining remote person with passport: %s%n", passportNumber);
        return remotePersons.get(passportNumber);
    }
}
