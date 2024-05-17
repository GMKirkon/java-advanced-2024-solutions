package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.person.IRemotePerson;
import info.kgeorgiy.ja.konovalov.bank.person.Person;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ActualBankTests {
    private static final Random random = new Random(1234786516234851234L);
    private static final List<Integer> rmiRegistriesPorts = List.of(239, 366, 533);
    private static final Bank[] banks = new Bank[rmiRegistriesPorts.size()];
    
    static Account addRandomAccountToBank(Bank bank) throws RemoteException {
        return bank.createAccount(AccountTests.ACCOUNT_IDS.get(random.nextInt(0, AccountTests.ACCOUNT_IDS.size())));
    }
    
    @BeforeAll
    public static void createRmi() {
        try {
            for (var port : rmiRegistriesPorts) {
                if (LocateRegistry.getRegistry(port) != null) {
                    LocateRegistry.createRegistry(port);
                }
            }
        } catch (RemoteException e) {
            System.out.println("Unexpected remote exception during instantiation, fix your network: " + e.getMessage());
        }
    }
    
    @AfterAll
    public static void shutdownBanks() {
        try {
            for (var bank : banks) {
                UnicastRemoteObject.unexportObject(bank, true);
            }
        } catch (RemoteException e) {
            System.out.println("Unexpected remote exception during instantiation, fix your network: " + e.getMessage());
        }
    }
    
    private static Bank getBankFromRmiByPort(int port) throws MalformedURLException, NotBoundException, RemoteException {
        return (Bank) Naming.lookup(String.format("//localhost:%d/bank", port));
    }
    
    @BeforeEach
    public void createBanks() {
        try {
            for (int i = 0; i < rmiRegistriesPorts.size(); i++) {
                recreateBank(i, RmiAccountsPolicy.values()[random.nextInt(0, 4)]);
            }
        } catch (RemoteException e) {
            System.out.println("Unexpected remote exception during instantiation, fix your network: " + e.getMessage());
        }
    }
    
    private static void recreateBank(int portInd, RmiAccountsPolicy policy) throws RemoteException {
        var port = rmiRegistriesPorts.get(portInd);
        banks[portInd] = new RemoteBank(rmiRegistriesPorts, policy);
        Bank bank = banks[portInd];
        UnicastRemoteObject.exportObject(bank, port);
        try {
            Naming.rebind(String.format("//localhost:%d/bank", port), bank);
        } catch (MalformedURLException e) {
            throw new AssertionError("Wrong hardcoded URL");
        }
    }
    
    @Test
    public void DefaultBank() throws RemoteException {
        int port = rmiRegistriesPorts.getFirst();
        Bank bank = new RemoteBank(port);
        String id = AccountTests.generateRandomAccountId();
        bank.createAccount(id);
        var account = bank.getAccount(id);
        AccountTests.checkAccountsBalance(account, 0);
    }
    
    @Test
    public void getBankFromRmi() {
        for (var port : rmiRegistriesPorts) {
            try {
                Bank bank = (Bank) Naming.lookup(String.format("//localhost:%d/bank", port));
            } catch (MalformedURLException e) {
                throw new AssertionError("Wrong hardcoded URL");
            } catch (RemoteException e) {
                throw new RuntimeException("fix network issues, remote exception occur" + e.getMessage());
            } catch (NotBoundException e) {
                throw new RuntimeException(String.format(
                        "Could not find bank at port: %d with error: %s",
                        port,
                        e.getMessage()
                ));
            }
        }
    }
    
    @Test
    public void checkDifferentRmi() {
        int port1 = rmiRegistriesPorts.getFirst();
        int port2 = rmiRegistriesPorts.getLast();
        try {
            Bank bank1 = getBankFromRmiByPort(port1);
            Bank bank2 = getBankFromRmiByPort(port2);
            
            String id = AccountTests.generateRandomAccountId();
            
            var account1 = bank1.createAccount(id);
            var personData = PersonsTests.generateRandomPersonData();
            Person person1 = bank1.createPerson(personData.name(), personData.surname(), personData.passport());
            
            Assertions.assertNull(bank2.getAccount(id));
            Assertions.assertNull(bank2.getRemotePerson(personData.passport()));
            Assertions.assertNull(bank2.getLocalPerson(personData.passport()));
            
            var account2 = bank2.createAccount(id);
            Person person2 = bank2.createPerson(personData.name(), personData.surname(), personData.passport());
            
            account1.addAmount(100);
            Assertions.assertEquals(100, account1.getAmount());
            Assertions.assertEquals(0, account2.getAmount());
        } catch (MalformedURLException e) {
            throw new AssertionError("Wrong hardcoded URL");
        } catch (RemoteException e) {
            throw new RuntimeException("fix network issues, remote exception occur" + e.getMessage());
        } catch (NotBoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    @Test
    public void creatingAccountForPersonFromBank() throws RemoteException {
        Bank bank = banks[0];
        var personData = PersonsTests.generateRandomPersonData();
        IRemotePerson person = bank.createPerson(personData.name(), personData.surname(), personData.passport());
        String id = AccountTests.generateRandomAccountId();
        
        bank.createAccountForPerson(id, person);
        Assertions.assertEquals(1, person.getAllAccounts().size());
        
        var account = person.getAccount(id);
        Assertions.assertEquals(0, account.getAmount());
        
        account.addAmount(100);
        var accountFromBank = bank.getAccount(PersonsTests.generateFullAccountIdForPerson(person, id));
        Assertions.assertEquals(100, account.getAmount());
        Assertions.assertEquals(100, accountFromBank.getAmount());
        
        accountFromBank.addAmount(200);
        Assertions.assertEquals(300, account.getAmount());
        Assertions.assertEquals(300, accountFromBank.getAmount());
    }
    
    @Test
    public void localRemoteTest() throws RemoteException {
        Bank bank = banks[0];
        var personData = PersonsTests.generateRandomPersonData();
        String id = AccountTests.generateRandomAccountId();
        IRemotePerson person = bank.createPerson(personData.name(), personData.surname(), personData.passport());
        
        var account = bank.createAccountForPerson(id, person);
        
        var localPerson1 = bank.getLocalPerson(personData.passport());
        account.addAmount(100);
        var localPerson2 = bank.getLocalPerson(personData.passport());
        account.addAmount(100);
        var localPerson3 = bank.getLocalPerson(personData.passport());
        account.addAmount(100);
        
        Assertions.assertEquals(0, localPerson1.getAccount(id).getAmount());
        Assertions.assertEquals(100, localPerson2.getAccount(id).getAmount());
        Assertions.assertEquals(200, localPerson3.getAccount(id).getAmount());
        Assertions.assertEquals(300, person.getAccount(id).getAmount());
    }
    
    @Test
    public void localSerializedTest() throws RemoteException {
        Bank bank = banks[0];
        var personData = PersonsTests.generateRandomPersonData();
        String id = AccountTests.generateRandomAccountId();
        IRemotePerson person = bank.createPerson(personData.name(), personData.surname(), personData.passport());
        
        var localPerson = bank.getLocalPerson(personData.passport());
        Assertions.assertThrows(
                NoSuchObjectException.class,
                () -> UnicastRemoteObject.unexportObject(localPerson, false)
        );
    }
    
    @Test
    public void remoteTest() throws RemoteException {
        Bank bank = banks[0];
        var personData = PersonsTests.generateRandomPersonData();
        bank.createPerson(personData.name(), personData.surname(), personData.passport());
        
        var localPerson = bank.getRemotePerson(personData.passport());
        Assertions.assertDoesNotThrow(
                () -> UnicastRemoteObject.unexportObject(localPerson, false)
        );
    }
}
