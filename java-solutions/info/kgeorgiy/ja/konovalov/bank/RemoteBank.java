package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.account.IRemoteAccount;
import info.kgeorgiy.ja.konovalov.bank.account.RemoteAccount;
import info.kgeorgiy.ja.konovalov.bank.account.TooMuchMoneyException;
import info.kgeorgiy.ja.konovalov.bank.person.LocalPerson;
import info.kgeorgiy.ja.konovalov.bank.person.Person;
import info.kgeorgiy.ja.konovalov.bank.person.RemotePerson;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class RemoteBank implements Bank {
    private static final Comparator<IRemoteAccount> accountOrder = (IRemoteAccount a, IRemoteAccount b) -> {
        try {
            return a.getId().compareTo(b.getId());
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    };
    private final ConcurrentMap<String, IRemoteAccount> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RemotePerson> remotePersons = new ConcurrentHashMap<>();
    private final RmiRegistriesScheduler scheduler;
    
    public RemoteBank(final int rmiRegistryExportingPort) {
        scheduler = RmiAccountsPolicy.SINGLE.getScheduler(List.of(rmiRegistryExportingPort));
    }
    
    public RemoteBank(List<Integer> ports, RmiAccountsPolicy policy) {
        scheduler = policy.getScheduler(ports);
    }
    
    @Override
    public IRemoteAccount createAccount(final String id) throws RemoteException {
        System.out.printf("Creating account %s%n", id);
        if (accounts.get(id) != null) {
            throw new IllegalArgumentException(String.format("account with provided id:%s is already created", id));
        }
        
        final IRemoteAccount account = new RemoteAccount(id);
        accounts.put(id, account);
        UnicastRemoteObject.exportObject(account, scheduler.getAccountPort());
        return account;
    }
    
    @Override
    public IRemoteAccount createAccountForPerson(final String id, final Person person) throws RemoteException {
        String fullId = String.format("%s:%s", person.getPassportNumber(), id);
        IRemoteAccount account = createAccount(fullId);
        return account;
    }
    
    @Override
    public void transfer(IRemoteAccount from, IRemoteAccount to, int amount) throws RemoteException {
        if (!accounts.containsValue(from)) {
            throw new IllegalArgumentException("Cannot transfer from account that is not registered in the bank");
        }
        
        if (!accounts.containsValue(to)) {
            throw new IllegalArgumentException("Cannot transfer to account that is not registered in the bank");
        }
        
        if (from == to) {
            throw new IllegalArgumentException("Transfers to the same account are not allowed");
        }
        
        if (amount < 0) {
            throw new IllegalArgumentException("Could not transfer negative amount of money");
        }
        
        var accounts = Stream.of(from, to).sorted(accountOrder).toList();
        synchronized (accounts.get(0)) {
            synchronized (accounts.get(1)) {
                from.addAmount(-amount);
                try {
                    to.addAmount(amount);
                } catch (TooMuchMoneyException e) {
                    from.addAmount(amount);
                    throw e;
                }
            }
        }
    }
    
    @Override
    public synchronized RemotePerson createPerson(String name, String surname, String passportNumber) throws RemoteException {
        System.out.printf("Creating person %s %s %s%n", name, surname, passportNumber);
        
        if (remotePersons.get(passportNumber) != null) {
            throw new IllegalArgumentException("Person with such passport is already registered");
        }
        
        int port = scheduler.getPersonPort();
        var person = new RemotePerson(name, surname, passportNumber, this);
        if (remotePersons.putIfAbsent(passportNumber, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
        }
        return person;
    }
    
    @Override
    public IRemoteAccount getAccount(final String id) {
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
