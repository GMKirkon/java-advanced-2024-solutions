package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.account.*;
import info.kgeorgiy.ja.konovalov.bank.account.TooMuchMoneyException;
import info.kgeorgiy.ja.konovalov.bank.person.ILocalPerson;
import info.kgeorgiy.ja.konovalov.bank.person.IRemotePerson;
import info.kgeorgiy.ja.konovalov.bank.person.LocalPerson;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ActualBankTests {
    
    private static final List<Integer> SIZES = List.of(10_000, 1_000, 300, 100, 5, 2, 1);
    private static final int MAX_THREADS = 100;
    private static final List<String> ACCOUNT_IDS = new ArrayList<>();
    private static final List<String> NAMES = new ArrayList<>();
    private static final List<String> SURNAMES = new ArrayList<>();
    private static final List<String> PASSPORT_DATA = new ArrayList<>();
    private static final int MAX_NUMBER_OF_PERSONS = 1000;
    private static final int MAX_NUMBER_OF_ACCOUNTS = 1000;
    private static final List<Integer> rmiRegistriesPorts = List.of(239, 366, 533);
    private static final Bank[] banks = new Bank[rmiRegistriesPorts.size()];
    private static final Person[] persons = new Person[MAX_NUMBER_OF_PERSONS];
    private static final Account[] accounts = new Account[MAX_NUMBER_OF_ACCOUNTS];
    private static final associatedAccountsWithPersonAndBank[] associatedAccounts = new associatedAccountsWithPersonAndBank[MAX_NUMBER_OF_ACCOUNTS];
    protected static Random random = new Random(2396942097832348976L);
    private static int createdPersons;
    private static int createdAccounts;
    
    private record associatedAccountsWithPersonAndBank(Person person, IRemoteAccount associatedAccount, Bank bank) {
    }
    
    private record PersonsData(String name, String surname, String passport) {
    }
    
    private static boolean canTransfer(associatedAccountsWithPersonAndBank account1, associatedAccountsWithPersonAndBank account2) {
        return account1.bank == account2.bank && account1.associatedAccount != account2.associatedAccount;
    }
    
    static void checkAccountsBalance(Account account, int expectedBalance) throws RemoteException {
        Assertions.assertEquals(account.getAmount(), expectedBalance);
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
    
    @BeforeAll
    public static void createRandomData() {
        fillArrayWithRandomStrings(NAMES);
        fillArrayWithRandomStrings(SURNAMES);
        fillArrayWithRandomStrings(PASSPORT_DATA);
        fillArrayWithRandomStrings(ACCOUNT_IDS);
    }
    
    private static void fillArrayWithRandomStrings(List<String> data) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 23;
        
        // https://www.baeldung.com/java-random-string
        for (int i = 0; i < MAX_NUMBER_OF_PERSONS; i++) {
            String generatedString = random.ints(leftLimit, rightLimit + 1)
                                           .limit(targetStringLength)
                                           .collect(
                                                   StringBuilder::new,
                                                   StringBuilder::appendCodePoint,
                                                   StringBuilder::append
                                           )
                                           .toString();
            
            data.add(generatedString);
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
    
    private static void recreateBank(int portInd, RmiAccountsPolicy policy) {
        var port = rmiRegistriesPorts.get(portInd);
        banks[portInd] = new RemoteBank(rmiRegistriesPorts, policy);
        Bank bank = banks[portInd];
        exportBank(bank, port);
    }
    
    static void exportBank(Bank bank, int port) {
        try {
            UnicastRemoteObject.exportObject(bank, port);
            try {
                Naming.rebind(String.format("//localhost:%d/bank", port), bank);
            } catch (MalformedURLException e) {
                throw new AssertionError("Wrong hardcoded URL");
            }
        } catch (RemoteException e) {
            throw new AssertionError("Cannot create bank, fix network " + e.getMessage());
        }
    }
    
    /* package-private */
    static PersonsData generatePersonData(int i, int j, int k) {
        return new PersonsData(
                NAMES.get(i),
                SURNAMES.get(j),
                PASSPORT_DATA.get(k)
        );
    }
    
    /* package-private */
    static PersonsData generateRandomPersonData() {
        return generatePersonData(
                random.nextInt(0, NAMES.size()),
                random.nextInt(0, SURNAMES.size()),
                random.nextInt(0, PASSPORT_DATA.size())
        );
    }
    
    /* package-private */
    static PersonsData generateNotInitializedPersonData() {
        var data = generatePersonData(
                random.nextInt(0, NAMES.size()),
                random.nextInt(0, SURNAMES.size()),
                random.nextInt(0, PASSPORT_DATA.size())
        );
        return new PersonsData("#NOT" + data.name, "#NOT" + data.surname, "#NOT" + data.passport);
    }
    
    /* package-private */
    static String generateFullAccountIdForPerson(Person person, String accountID) throws RemoteException {
        return person.getPassportNumber() + ":" + accountID;
    }
    
    private static void createBanks() {
        for (int i = 0; i < rmiRegistriesPorts.size(); i++) {
            recreateBank(i, RmiAccountsPolicy.values()[random.nextInt(0, 4)]);
        }
    }
    
    Account generateRandomAccount(String id) {
        return new RemoteAccount(ACCOUNT_IDS.get(random.nextInt(0, ACCOUNT_IDS.size())));
    }
    
    String generateRandomAccountId() {
        return ACCOUNT_IDS.get(random.nextInt(0, ACCOUNT_IDS.size()));
    }
    
    String generateNotInitializedAccountId() {
        return "#NOT" + ACCOUNT_IDS.get(random.nextInt(0, ACCOUNT_IDS.size()));
    }
    
    private void createPersonsAndAccounts() {
        createdPersons = 0;
        createdAccounts = 0;
        Arrays.fill(accounts, null);
        Arrays.fill(associatedAccounts, null);
        Arrays.fill(persons, null);
        
        for (int i = 0; i < MAX_NUMBER_OF_PERSONS; i++) {
            Bank bank = banks[random.nextInt(0, banks.length)];
            PersonsData data = generateRandomPersonData();
            try {
                var person = bank.createPerson(data.name, data.surname, data.passport);
                persons[createdPersons++] = person;
                int tryCreateAccountsNumbers = random.nextInt(
                        0,
                        Integer.min(10, MAX_NUMBER_OF_ACCOUNTS - createdAccounts)
                );
                
                Assertions.assertTrue(() -> {
                    try {
                        return bank.getRemotePerson(data.passport()) == person;
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                });
                
                for (int j = 0; j < tryCreateAccountsNumbers; j++) {
                    try {
                        IRemoteAccount account = person.addAccount(ACCOUNT_IDS.get(random.nextInt(
                                0,
                                ACCOUNT_IDS.size()
                        )));
                        
                        Assertions.assertTrue(() -> {
                            try {
                                return bank.getAccount(account.getId()) == account;
                            } catch (RemoteException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        
                        accounts[createdAccounts] = account;
                        associatedAccounts[createdAccounts] = new associatedAccountsWithPersonAndBank(
                                person,
                                account,
                                bank
                        );
                        ++createdAccounts;
                    } catch (IllegalArgumentException ignored) {
                    
                    }
                }
            } catch (IllegalArgumentException ignored) {
            } catch (RemoteException e) {
                throw new AssertionError("Fix network " + e.getMessage());
            }
        }
    }
    
    Account addRandomAccountToBank(Bank bank) throws RemoteException {
        return bank.createAccount(ACCOUNT_IDS.get(random.nextInt(0, ACCOUNT_IDS.size())));
    }
    
    @BeforeEach
    public void createEnvironment() {
        createBanks();
        createPersonsAndAccounts();
    }
    
    /* package-private */ Account createAccountForPerson(Person person, String bankId) throws RemoteException {
        return new RemoteAccount(generateAccountId(person, bankId));
    }
    
    
    /* package-private */ String generateAccountId(Person person, String bankId) throws RemoteException {
        return String.format("%s:%s", person.getPassportNumber(), bankId);
    }
    
    
    /* package-private */ Person getRemotePerson(Bank bank) throws RemoteException {
        for (int i = 0; i < createdAccounts; i++) {
            var account = associatedAccounts[i];
            if (account.bank == bank) {
                return account.person;
            }
        }
        throw new AssertionError("Cannot find person from bank");
    }
    
    /* package-private */ Person getDefaultRemotePerson() throws RemoteException {
        return getRemotePerson(banks[0]);
    }
    
    
    /* package-private */ String getRandomAccountId() {
        return ACCOUNT_IDS.get(random.nextInt(0, ACCOUNT_IDS.size()));
    }
    
    /* package-private */ String getRandomFullAccountIdForPerson(Person person) throws RemoteException {
        return generateFullAccountIdForPerson(
                person,
                ACCOUNT_IDS.get(random.nextInt(
                        0,
                        ACCOUNT_IDS.size()
                ))
        );
    }
    
    protected final List<Integer> randomIntegerList(final int size) {
        final List<Integer> pool = random.ints(Math.min(size, 1_000)).boxed().toList();
        return random.ints(0, pool.size()).limit(size).mapToObj(pool::get).toList();
    }
    
    void testPerson(Person person, PersonsData data) throws RemoteException {
        Assertions.assertEquals(person.getName(), data.name);
        Assertions.assertEquals(person.getSurname(), data.surname);
        Assertions.assertEquals(person.getPassportNumber(), data.passport);
        Assertions.assertEquals(person.getAllAccounts().size(), 0);
    }
    
    void testAccount(Account account, String id, int balance) throws RemoteException {
        Assertions.assertEquals(account.getId(), id);
        Assertions.assertEquals(account.getAmount(), balance);
    }
    
    Account getNonNullAccountFromPerson(Person person, String accountID) throws RemoteException {
        var account = person.getAccount(accountID);
        Assertions.assertNotNull(account);
        return account;
    }
    
    @Test
    public void serverTest() throws MalformedURLException, NotBoundException, RemoteException {
        Server.testingMain("240", "1");
        
        Assertions.assertThrows(RuntimeException.class, () -> Server.testingMain((String[])null));
        Assertions.assertThrows(RuntimeException.class, () -> Server.testingMain((String)null));
        Assertions.assertThrows(RuntimeException.class, () -> Server.testingMain("-1"));
        Assertions.assertThrows(RuntimeException.class, () -> Server.testingMain("-1 2"));
        Assertions.assertThrows(RuntimeException.class, () -> Server.testingMain("3 100"));
        Assertions.assertThrows(RuntimeException.class, () -> Server.testingMain("-1 -1"));
        Assertions.assertThrows(RuntimeException.class, () -> Server.testingMain("10000000 3"));
        Assertions.assertThrows(RuntimeException.class, () -> Server.testingMain("fizz buzz"));
        
        var bank = getBankFromRmiByPort(240);
        Assertions.assertNotNull(bank);
    }
    
    
    @Test
    public void clientTest() throws RemoteException {
        var personData = generateNotInitializedPersonData();
        var accountID = generateNotInitializedAccountId();
        
        Bank bank = banks[0];
        
        Client.testingMain(personData.name, personData.surname, personData.passport, accountID, "100");
        
        var createdPerson = bank.getRemotePerson(personData.passport);
        Assertions.assertNotNull(createdPerson);
        Assertions.assertEquals(1, createdPerson.getAllAccounts().size());
        var account = createdPerson.getAccount(accountID);
        Assertions.assertEquals(account.getAmount(), 100);
        
        Assertions.assertThrows(RuntimeException.class, () -> Client.testingMain((String[]) null));
        Assertions.assertThrows(RuntimeException.class, () -> Client.testingMain((String) null));
        Assertions.assertThrows(RuntimeException.class, () -> Client.testingMain("123", "-123", "kek"));
        Assertions.assertThrows(RuntimeException.class, () -> Client.testingMain("foo", "foo", "foo", "-1", "-1"));
        Assertions.assertThrows(RuntimeException.class, () -> Client.testingMain("foo", "foo", "foo", "-1", "kek"));
    }
    
    @Test
    public void createSingleAccount() throws RemoteException {
        Bank bank = banks[0];
        var person = getRemotePerson(bank);
        var person2 = getRemotePerson(banks[1]);
        
        Assertions.assertNull(bank.getLocalPerson(person2.getPassportNumber()));
        
        String accountID = generateNotInitializedAccountId();
        String fullAccountID = generateFullAccountIdForPerson(person, accountID);
        int currentSize = person.getAllAccounts().size();
        
        person.addAccount(accountID);
        Assertions.assertEquals(currentSize + 1, person.getAllAccounts().size());
        
        var account = getNonNullAccountFromPerson(person, accountID);
        testAccount(account, fullAccountID, 0);
        
        account.setAmount(100);
        account = getNonNullAccountFromPerson(person, accountID);
        
        testAccount(account, fullAccountID, 100);
    }
    
    @Test
    public void testLocal() throws RemoteException {
        Bank bank = banks[0];
        var remotePerson = getRemotePerson(bank);
        var localPerson1 = new LocalPerson(remotePerson);
        
        String id = getRandomAccountId();
        
        int initialAccounts = remotePerson.getAllAccounts().size();
        remotePerson.addAccount(id);
        
        
        Assertions.assertEquals(initialAccounts + 1, remotePerson.getAllAccounts().size());
        Assertions.assertEquals(initialAccounts, localPerson1.getAllAccounts().size());
        
        var localPerson2 = new LocalPerson(remotePerson);
        Assertions.assertEquals(initialAccounts + 1, localPerson2.getAllAccounts().size());
        
        var account = remotePerson.getAccount(id);
        account.addAmount(1000);
        
        Assertions.assertEquals(1000, remotePerson.getAccount(id).getAmount());
        Assertions.assertEquals(0, localPerson2.getAccount(id).getAmount());
    }
    
    @Test
    public void multiThreadedPersonOperation() throws RemoteException {
        ExecutorService exectors = Executors.newFixedThreadPool(random.nextInt(2, MAX_THREADS));
        Bank bank = banks[0];
        
        var person = getRemotePerson(bank);
        
        ConcurrentHashMap<String, Integer> actualAccountsValue = new ConcurrentHashMap<>();
        AtomicReference<Exception> exception = new AtomicReference<>();
        
        final UnaryOperator<Exception> addSuppressedToTotal = (Exception e) -> exception.getAndUpdate(cur -> {
            if (cur == null) {
                return e;
            } else {
                cur.addSuppressed(e);
                return cur;
            }
        });
        
        int initialSize = person.getAllAccounts().size();
        
        IntStream.range(0, 10 * MAX_THREADS).forEach(ind -> exectors.submit(() -> {
            try {
                String id = String.valueOf(ind);
                String fullId = generateFullAccountIdForPerson(person, id);
                person.addAccount(id);
                actualAccountsValue.put(fullId, 0);
                IntStream.range(0, 10).forEach(x -> {
                    int value = random.nextInt(-100, 100);
                    
                    var current = actualAccountsValue.compute(fullId, (k, v) -> v + value);
                    if (current < 0) {
                        Assertions.assertThrows(
                                InsufficientFundsException.class,
                                () -> getNonNullAccountFromPerson(person, id).addAmount(value)
                        );
                        actualAccountsValue.compute(fullId, (k, v) -> v - value);
                    } else {
                        try {
                            getNonNullAccountFromPerson(person, id).addAmount(value);
                        } catch (Exception e) {
                            addSuppressedToTotal.apply(e);
                        }
                    }
                });
            } catch (Exception e) {
                addSuppressedToTotal.apply(e);
            }
        }));
        exectors.close();
        
        Assertions.assertNull(exception.get());
        Assertions.assertEquals(10 * MAX_THREADS, actualAccountsValue.size());
        Assertions.assertEquals(10 * MAX_THREADS + initialSize, person.getAllAccounts().size());
        person.getAllAccounts().forEach((key, value) -> {
            try {
                if (actualAccountsValue.containsKey(key)) {
                    Assertions.assertEquals(actualAccountsValue.get(key), value.getAmount());
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    @Test
    public void defaultBank() throws RemoteException {
        int port = rmiRegistriesPorts.getFirst();
        Bank bank = new RemoteBank(port);
        String id = generateRandomAccountId();
        bank.createAccount(id);
        var account = bank.getAccount(id);
        checkAccountsBalance(account, 0);
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
    public void checkDifferentRmi() throws RemoteException {
        Bank bank1 = banks[0];
        Bank bank2 = banks[1];
        
        String id = generateNotInitializedAccountId();
        
        var account1 = bank1.createAccount(id);
        var personData = generateNotInitializedPersonData();
        Person person1 = bank1.createPerson(personData.name(), personData.surname(), personData.passport());
        
        Assertions.assertNotEquals(account1, bank2.getAccount(id));
        Assertions.assertNotEquals(person1, bank2.getRemotePerson(personData.passport()));
        
        
        var account2 = bank2.createAccount(id);
        Person person2 = bank2.createPerson(personData.name(), personData.surname(), personData.passport());
        
        Assertions.assertNotEquals(account2, bank1.getAccount(id));
        Assertions.assertNotEquals(person2, bank1.getRemotePerson(personData.passport()));
        
        account1.addAmount(100);
        Assertions.assertEquals(100, account1.getAmount());
        Assertions.assertEquals(0, account2.getAmount());
        
    }
    
    @Test
    public void creatingAccountBankConsistencyTest() throws RemoteException {
        Bank bank = banks[0];
        var personData = generateNotInitializedPersonData();
        IRemotePerson person = bank.createPerson(personData.name(), personData.surname(), personData.passport());
        String id = generateNotInitializedAccountId();
        
        person.addAccount(id);
        Assertions.assertEquals(1, person.getAllAccounts().size());
        
        var account = person.getAccount(id);
        Assertions.assertEquals(0, account.getAmount());
        
        account.addAmount(100);
        var accountFromBank = bank.getAccount(generateFullAccountIdForPerson(person, id));
        Assertions.assertEquals(100, account.getAmount());
        Assertions.assertEquals(100, accountFromBank.getAmount());
        
        accountFromBank.addAmount(200);
        Assertions.assertEquals(300, account.getAmount());
        Assertions.assertEquals(300, accountFromBank.getAmount());
    }
    
    @Test
    public void localRemoteTest() throws RemoteException {
        Bank bank = banks[0];
        var personData = generateNotInitializedPersonData();
        String id = generateNotInitializedAccountId();
        IRemotePerson person = bank.createPerson(personData.name(), personData.surname(), personData.passport());
        
        var account = person.addAccount(id);
        
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
        var personData = generateNotInitializedPersonData();
        String id = generateNotInitializedAccountId();
        IRemotePerson person = bank.createPerson(personData.name(), personData.surname(), personData.passport());
        
        var localPerson = bank.getLocalPerson(personData.passport());
        Assertions.assertThrows(
                NoSuchObjectException.class,
                () -> UnicastRemoteObject.unexportObject(localPerson, false)
        );
    }
    
    @Test
    public void remoteActualRemoteTest() throws RemoteException {
        Bank bank = banks[0];
        var personData = generateNotInitializedPersonData();
        bank.createPerson(personData.name(), personData.surname(), personData.passport());
        
        var localPerson = bank.getRemotePerson(personData.passport());
        Assertions.assertDoesNotThrow(
                () -> UnicastRemoteObject.unexportObject(localPerson, false)
        );
    }
    
    
    @Test
    public void defaultCtorAccount() {
        var account = new RemoteAccount("Turmax");
        Assertions.assertEquals(account.getId(), "Turmax");
        Assertions.assertEquals(account.getAmount(), 0);
    }
    
    @Test
    public void copyCtor() throws RemoteException {
        var account = new RemoteAccount("Turmax");
        var accountCopy = new RemoteAccount(account);
        
        Assertions.assertEquals(account.getId(), "Turmax");
        Assertions.assertEquals(account.getAmount(), 0);
        Assertions.assertEquals(accountCopy.getId(), "Turmax");
        Assertions.assertEquals(accountCopy.getAmount(), 0);
    }
    
    @Test
    public void singleAddition() {
        var account = new RemoteAccount("Turmax");
        try {
            account.addAmount(1_000_000_000);
        } catch (InsufficientFundsException e) {
            Assertions.fail("adding million to zero should not throw");
        }
        Assertions.assertEquals(account.getAmount(), 1_000_000_000);
    }
    
    @Test
    public void set() {
        var account = new RemoteAccount("Turmax");
        for (var size : SIZES) {
            var currentOperations = randomIntegerList(size);
            int actual = account.getAmount();
            for (var setted : currentOperations) {
                if (setted < 0) {
                    Assertions.assertThrows(IllegalArgumentException.class, () -> account.setAmount(setted));
                } else {
                    account.setAmount(setted);
                    actual = setted;
                }
            }
            Assertions.assertEquals(actual, account.getAmount());
        }
    }
    
    @Test
    public void overflow() {
        var account = new RemoteAccount("Turmax");
        account.addAmount(2_000_000_000);
        Assertions.assertThrows(RuntimeException.class, () -> account.addAmount(2_000_000_000));
        Assertions.assertEquals(account.getAmount(), 2_000_000_000);
    }
    
    @Test
    public void negativeAmount() {
        var account = new RemoteAccount("Turmax");
        Assertions.assertThrows(InsufficientFundsException.class, () -> account.addAmount(-1));
    }
    
    @Test
    public void threadSafeAddition() {
        try {
            for (var current_size : SIZES) {
                var account = getRandomAccount();
                ExecutorService exectors = Executors.newFixedThreadPool(random.nextInt(2, MAX_THREADS));
                AtomicInteger actualResult = new AtomicInteger(account.getAmount());
                IntStream.range(0, current_size).forEach(ind -> exectors.submit(() -> {
                    int value = random.nextInt(0, 100);
                    actualResult.addAndGet(value);
                    try {
                        account.addAmount(value);
                    } catch (RemoteException e) {
                        throw new RuntimeException(e);
                    }
                }));
                exectors.close();
                Assertions.assertEquals(actualResult.get(), account.getAmount());
            }
        } catch (Exception e) {
            throw new AssertionError("Fix network " + e.getMessage());
        }
    }
    
    private List<associatedAccountsWithPersonAndBank> getAssociatedAccounts(Bank bank) {
        return Arrays.stream(associatedAccounts)
                     .limit(createdAccounts)
                     .filter(x -> x.bank == bank)
                     .distinct()
                     .toList();
    }
    
    private associatedAccountsWithPersonAndBank getAssociatedAccounts(Bank bank, int i) {
        return getAssociatedAccounts(bank).get(i);
    }
    
    private Bank getRandomBank() {
        return banks[random.nextInt(0, banks.length)];
    }
    
    private Account getRandomAccount() {
        return accounts[random.nextInt(0, accounts.length)];
    }
    
    @Test
    public void singleTransfer() throws RemoteException {
        Bank bank = getRandomBank();
        associatedAccountsWithPersonAndBank account1 = getAssociatedAccounts(bank, 0);
        associatedAccountsWithPersonAndBank account2 = getAssociatedAccounts(bank, 1);
        
        account1.associatedAccount.addAmount(1_000_000_000);
        account2.associatedAccount.addAmount(1_000_000_000);
        
        
        Assertions.assertEquals(account1.associatedAccount.getAmount(), 1_000_000_000);
        Assertions.assertEquals(account2.associatedAccount.getAmount(), 1_000_000_000);
        
        bank.transfer(account2.associatedAccount, account1.associatedAccount, 1_000_000_000);
        Assertions.assertEquals(account1.associatedAccount.getAmount(), 2_000_000_000);
        
        
        Assertions.assertThrows(TooMuchMoneyException.class, () -> account1.associatedAccount.addAmount(1_000_000_000));
    }
    
    @Test
    public void invalidTransfers() throws RemoteException {
        Bank bank = getRandomBank();
        var account1 = getAssociatedAccounts(bank, 0).associatedAccount;
        var account2 = getAssociatedAccounts(bank, 1).associatedAccount;
        account1.addAmount(1_000_000_000);
        
        Assertions.assertThrows(
                InsufficientFundsException.class,
                () -> bank.transfer(account1, account2, 2_000_000_000)
        );
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> bank.transfer(account1, account2, -1)
        );
        
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> bank.transfer(account1, account1, 1)
        );
        
        account1.addAmount(1_000_000_000);
        account2.addAmount(1_000_000_000);
        
        Assertions.assertThrows(
                TooMuchMoneyException.class,
                () -> bank.transfer(account1, account2, 2_000_000_000)
        );
    }
    
    @Test
    public void unsupportedOpertaionsTest() throws RemoteException {
        Person remotePerson = getRemotePerson(banks[0]);
        ILocalPerson person = new LocalPerson(remotePerson);
        
        Assertions.assertThrows(UnsupportedOperationException.class, () -> person.addAccount("123"));
        
        ILocalAccount account = new LocalAccount(accounts[0]);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> account.addAmount(100));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> account.setAmount(100));
    }
    
    @Test
    public void illegalOpertaionsTest() {
        Bank bank = banks[0];
        var account = getAssociatedAccounts(bank, 0).associatedAccount;
        var accountInDifferentBank = getAssociatedAccounts(banks[1], 0).associatedAccount;
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> bank.transfer(account, account, 10));
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> bank.transfer(account, accountInDifferentBank, 10)
        );
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> bank.transfer(accountInDifferentBank, account, 10)
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> bank.transfer(account, account, -1));
    }
    
    
    @Test
    public void randomTransfers() throws RemoteException {
        randomTransfers(false);
    }
    
    @Test
    public void randomParallelTransfers() throws RemoteException {
        randomTransfers(true);
    }
    
    private void randomTransfers(boolean parallel) throws RemoteException {
        for (int size : SIZES) {
            Bank bank = getRandomBank();
            var accounts = getAssociatedAccounts(bank);
            ConcurrentHashMap<Account, Integer> realBalances = new ConcurrentHashMap<>();
            for (var u : accounts) {
                int initialValue = random.nextInt(0, 1_000_000_000);
                u.associatedAccount.setAmount(initialValue);
                realBalances.put(u.associatedAccount, initialValue);
            }
            
            
            IntConsumer fun = ind -> {
                int pointer = random.nextInt(0, accounts.size());
                int nxt = random.nextInt(0, accounts.size());
                var account1 = accounts.get(pointer);
                var account2 = accounts.get(nxt);
                try {
                    int value = random.nextInt(-1000, 1000);
                    if (canTransfer(account1, account2) && account1.bank == bank && value >= 0) {
                        bank.transfer(account1.associatedAccount, account2.associatedAccount, value);
                        realBalances.compute(account1.associatedAccount, (acc, bal) -> bal - value);
                        realBalances.compute(account2.associatedAccount, (acc, bal) -> bal + value);
                    } else {
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () -> bank.transfer(
                                        account1.associatedAccount,
                                        account2.associatedAccount,
                                        value
                                )
                        );
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            };
            
            if (parallel) {
                IntStream.range(0, size).parallel().forEach(fun);
            } else {
                IntStream.range(0, size).forEach(fun);
            }
            
            realBalances.forEach((key, value) -> {
                try {
                    Assertions.assertEquals(value, key.getAmount());
                } catch (RemoteException e) {
                    throw new AssertionError("Never happens");
                }
            });
        }
    }
    
    @Test
    public void createManyPersonsWithAccounts() throws RemoteException {
        Bank bank = new RemoteBank(239);
        exportBank(bank, 239);
        List<PersonsData> data = IntStream.range(0, 1000).mapToObj(x -> {
            var personData = generateRandomPersonData();
            return new PersonsData(
                    personData.name + "$" + x,
                    personData.surname + "$" + x,
                    personData.passport + "$" + x
            );
        }).toList();
        List<String> ids = IntStream.range(0, 10).mapToObj(x -> getRandomAccountId()).distinct().toList();
        
        List<Person> persons = new ArrayList<>(Collections.nCopies(data.size(), null));
        IntConsumer creatingPersons = (ind) -> {
            var currentPersonsData = data.get(ind);
            try {
                var person = bank.createPerson(
                        currentPersonsData.name,
                        currentPersonsData.surname,
                        currentPersonsData.passport
                );
                persons.set(ind, person);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
        
        BiConsumer<Integer, Integer> creatingAccounts = (ind1, ind2) -> {
            var person = persons.get(ind1);
            var accountId = ids.get(ind2);
            try {
                person.addAccount(accountId);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
        
        IntStream.range(0, data.size()).parallel().forEach(creatingPersons);
        IntStream.range(0, data.size()).parallel().forEach(ind1 -> {
            IntStream.range(0, ids.size()).parallel().forEach(ind2 -> {
                creatingAccounts.accept(ind1, ind2);
            });
        });
        
        for (int i = 0; i < data.size(); i++) {
            var person = persons.get(i);
            Assertions.assertNotNull(person);
            var personsValidData = data.get(i);
            Assertions.assertEquals(personsValidData.name, person.getName());
            Assertions.assertEquals(personsValidData.surname, person.getSurname());
            Assertions.assertEquals(personsValidData.passport, person.getPassportNumber());
            
            Assertions.assertEquals(ids.size(), person.getAllAccounts().size());
        }
    }
}
