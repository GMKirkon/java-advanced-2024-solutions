package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.account.InsufficientFundsException;
import info.kgeorgiy.ja.konovalov.bank.account.RemoteAccount;
import info.kgeorgiy.ja.konovalov.bank.person.LocalPerson;
import info.kgeorgiy.ja.konovalov.bank.person.Person;
import info.kgeorgiy.ja.konovalov.bank.person.RemotePerson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class PersonsTests {
    
    protected static final int MAX_THREADS = 10;
    protected static final List<String> NAMES = List.of(
            "kgeorgiy",
            "kgrigoriy",
            "Andrewtza",
            "300iq",
            "tourist",
            "benQ",
            "orzderwang",
            "jiangly",
            "Василий",
            "عبد",
            "\uD83D\uDE24\uD83D\uDE29\uD83E\uDD7A\uD83D\uDE15\uD83D\uDE1F"
    );
    protected static final List<String> SURNAMES = List.of(
            "Korneev",
            "Vedernikov",
            "Budin",
            "Stankevich",
            "Петров",
            "Баширов",
            "عبّاس",
            "عبد الرشيد"
    );
    protected static final List<String> PASSPORT_DATA = List.of(
            "12345678",
            "87654321",
            "26511456",
            "9823123"
    );
    
    private static final int DEFAULT_WORKING_RMI_PORT = 1125;
    protected static Random random = new Random(3128751231235123451L);
    
    /* package-private */ record PersonsData(String name, String surname, String passport) {
    }
    
    @BeforeAll
    public static void createRmi() throws RemoteException {
        if (LocateRegistry.getRegistry(DEFAULT_WORKING_RMI_PORT) != null) {
            LocateRegistry.createRegistry(DEFAULT_WORKING_RMI_PORT);
        }
    }
    
    /* package-private */ Account createValidAccountForPerson(Person person, String bankId) throws RemoteException {
        return new RemoteAccount(generateValidAccountId(person, bankId));
    }
    
    /* package-private */ Account createInvalidAccountForPerson(Person person, String bankId) throws RemoteException {
        return new RemoteAccount(generateInvalidAccountId(person, bankId));
    }
    
    /* package-private */ String generateValidAccountId(Person person, String bankId) throws RemoteException {
        return String.format("%s:%s", person.getPassportNumber(), bankId);
    }
    
    /* package-private */ String generateInvalidAccountId(Person person, String bankId) throws RemoteException {
        return String.format("%s:6%s", person.getPassportNumber(), bankId);
    }
    
    /* package-private */ static PersonsData generatePersonData(int i, int j, int k) {
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
    
    /* package-private */ RemotePerson getRemotePerson(int i, int j, int k) throws RemoteException {
        var personsData = generatePersonData(i, j, k);
        return new RemotePerson(
                personsData.name,
                personsData.surname,
                personsData.passport
        );
    }
    
    /* package-private */ RemotePerson getDefaultRemotePerson() throws RemoteException {
        return getRemotePerson(0, 0, 0);
    }
    
    /* package-private */ RemotePerson getRandomRemotePerson() throws RemoteException {
        return getRemotePerson(
                random.nextInt(0, NAMES.size()),
                random.nextInt(0, SURNAMES.size()),
                random.nextInt(0, PASSPORT_DATA.size())
        );
    }
    
    /* package-private */ String getRandomAccountId() {
        return AccountTests.ACCOUNT_IDS.get(random.nextInt(0, AccountTests.ACCOUNT_IDS.size()));
    }
    
    /* package-private */
    static String generateFullAccountIdForPerson(Person person, String accountID) throws RemoteException {
        return person.getPassportNumber() + ":" + accountID;
    }
    
    /* package-private */ String getRandomFullAccountIdForPerson(Person person) throws RemoteException {
        return generateFullAccountIdForPerson(
                person,
                AccountTests.ACCOUNT_IDS.get(random.nextInt(
                        0,
                        AccountTests.ACCOUNT_IDS.size()
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
    public void defaultCtor() throws RemoteException {
        testPerson(
                getDefaultRemotePerson(),
                generatePersonData(0, 0, 0)
        );
    }
    
    @Test
    public void createSingleAccount() throws RemoteException {
        var person = getRandomRemotePerson();
        String accountID = getRandomAccountId();
        String fullAccountID = generateFullAccountIdForPerson(person, accountID);
        
        var createdInitialAccount = createValidAccountForPerson(person, accountID);
        person.addAccount(createdInitialAccount, accountID);
        Assertions.assertEquals(person.getAllAccounts().size(), 1);
        
        var account = getNonNullAccountFromPerson(person, accountID);
        testAccount(account, fullAccountID, 0);
        
        account.setAmount(100);
        account = getNonNullAccountFromPerson(person, accountID);
        
        testAccount(account, fullAccountID, 100);
    }
    
    @Test
    public void createInvalidSingleAccount() throws RemoteException {
        var person = getRandomRemotePerson();
        
        String accountId = getRandomAccountId();
        var createdInitialAccount = createInvalidAccountForPerson(person, accountId);
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> person.addAccount(createdInitialAccount, accountId)
        );
        Assertions.assertEquals(person.getAllAccounts().size(), 0);
    }
    
    @Test
    public void testLocal() throws RemoteException {
        var remotePerson = getRandomRemotePerson();
        var localPerson1 = new LocalPerson(remotePerson);
        
        String id = getRandomAccountId();
        var createdInitialAccount = createValidAccountForPerson(remotePerson, id);
        remotePerson.addAccount(createdInitialAccount, id);
        
        Assertions.assertEquals(remotePerson.getAllAccounts().size(), 1);
        Assertions.assertEquals(localPerson1.getAllAccounts().size(), 0);
        
        var localPerson2 = new LocalPerson(remotePerson);
        Assertions.assertEquals(localPerson2.getAllAccounts().size(), 1);
        
        var account = remotePerson.getAccount(id);
        account.addAmount(1000);
        
        Assertions.assertEquals(remotePerson.getAccount(id).getAmount(), 1000);
        Assertions.assertEquals(localPerson2.getAccount(id).getAmount(), 0);
    }
    
    @Test
    public void multiThreadedPersonOperation() throws RemoteException {
        ExecutorService exectors = Executors.newFixedThreadPool(random.nextInt(2, MAX_THREADS));
        var person = getRandomRemotePerson();
        
        ConcurrentHashMap<String, Integer> actualAccountsValue = new ConcurrentHashMap<>();
        
        IntStream.range(0, 100).forEach(ind -> exectors.submit(() -> {
            try {
                String id = String.valueOf(ind);
                String fullId = generateFullAccountIdForPerson(person, id);
                var createdInitialAccount = createValidAccountForPerson(person, id);
                person.addAccount(createdInitialAccount, id);
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
                        } catch (RemoteException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }));
        exectors.close();
        
        Assertions.assertEquals(100, actualAccountsValue.size());
        Assertions.assertEquals(person.getAllAccounts().size(), 100);
        person.getAllAccounts().entrySet().forEach(entry -> {
            try {
                System.out.println(entry.getKey());
                Assertions.assertEquals(actualAccountsValue.get(entry.getKey()), entry.getValue().getAmount());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
