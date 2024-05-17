package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.account.InsufficientFundsException;
import info.kgeorgiy.ja.konovalov.bank.account.RemoteAccount;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class AccountTests {
    protected static final List<Integer> SIZES = java.util.List.of(10_000, 5, 2, 1);
    protected static final int MAX_THREADS = 10;
    protected static Random random = new Random(2396942097832348976L);
    
    protected static List<Integer> randomIntegerList(final int size) {
        final List<Integer> pool = random.ints(Math.min(size, 1_000_000)).boxed().toList();
        return random.ints(0, pool.size()).limit(size).mapToObj(pool::get).toList();
    }
    
    static void checkAccountsBalance(Account account, int expectedBalance) throws RemoteException {
        Assertions.assertEquals(account.getAmount(), expectedBalance);
    }
    
    protected static final List<String> ACCOUNT_IDS = List.of(
            "aksdjf",
            "879653",
            "187652123",
            "andrewtza",
            "SecondThread"
    );
    
    static Account generateRandomAccount(String id) {
        return new RemoteAccount(ACCOUNT_IDS.get(random.nextInt(0, ACCOUNT_IDS.size())));
    }
    
    static String generateRandomAccountId() {
        return ACCOUNT_IDS.get(random.nextInt(0, ACCOUNT_IDS.size()));
    }
    
    @Test
    public void defaultCtor() {
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
            currentOperations.forEach(account::setAmount);
            Assertions.assertEquals(account.getAmount(), currentOperations.getLast());
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
        for (var current_size : SIZES) {
            var account = new RemoteAccount("Turmax");
            Assertions.assertEquals(account.getAmount(), 0);
            ExecutorService exectors = Executors.newFixedThreadPool(random.nextInt(2, MAX_THREADS));
            AtomicInteger actualResult = new AtomicInteger(0);
            IntStream.range(0, current_size).forEach(ind -> exectors.submit(() -> {
                int value = random.nextInt(0, 100);
                actualResult.addAndGet(value);
                account.addAmount(value);
            }));
            exectors.close();
            Assertions.assertEquals(actualResult.get(), account.getAmount());
        }
    }
    
    @Test
    public void singleTransfer() throws RemoteException {
        var accountTur = new RemoteAccount("Turmax");
        var accountKir = new RemoteAccount("Kirkon");
        accountTur.addAmount(2_000_000_000);
        accountTur.transferToAnotherAccount(1_000_000_000, accountKir);
        
        checkAccountsBalance(accountTur, 1_000_000_000);
        checkAccountsBalance(accountKir, 1_000_000_000);
    }
    
    @Test
    public void invalidTransfer() {
        var accountTur = new RemoteAccount("Turmax");
        var accountKir = new RemoteAccount("Kirkon");
        accountTur.addAmount(1_000_000_000);
        Assertions.assertThrows(
                InsufficientFundsException.class,
                () -> accountTur.transferToAnotherAccount(2_000_000_000, accountKir)
        );
    }
    
    @Test
    public void negativeTransfer() {
        var accountTur = new RemoteAccount("Turmax");
        var accountKir = new RemoteAccount("Kirkon");
        accountTur.addAmount(1_000_000_000);
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> accountTur.transferToAnotherAccount(-1, accountKir)
        );
    }
    
    @Test
    public void selfTransfer() {
        var accountTur = new RemoteAccount("Turmax");
        accountTur.addAmount(1_000_000_000);
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> accountTur.transferToAnotherAccount(1_000_000_000, accountTur)
        );
    }
    
    @Test
    public void randomTransfers() {
        randomTransfers(false);
    }
    
    @Test
    public void randomParallelTransfers() {
        randomTransfers(true);
    }
    
    private void randomTransfers(boolean parallel) {
        var accountTur = new RemoteAccount("Turmax");
        var accountKir = new RemoteAccount("Kirkon");
        accountTur.addAmount(1_000_000_000);
        accountKir.addAmount(1_000_000_000);
        var accounts = List.of(accountTur, accountKir);
        var balances = List.of(new AtomicInteger(1_000_000_000), new AtomicInteger(1_000_000_000));
        
        IntConsumer fun = ind -> {
            int pointer = random.nextInt(0, 2);
            int nxt = pointer ^ 1;
            var account1 = accounts.get(pointer);
            var account2 = accounts.get(nxt);
            try {
                int value = random.nextInt(0, 1000);
                account1.transferToAnotherAccount(value, account2);
                balances.get(pointer).addAndGet(-value);
                balances.get(nxt).addAndGet(+value);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        };
        
        if (parallel) {
            IntStream.range(0, 100).parallel().forEach(fun);
        } else {
            IntStream.range(0, 100).forEach(fun);
        }
        
        Assertions.assertEquals(balances.get(0).get(), accountTur.getAmount());
        Assertions.assertEquals(balances.get(1).get(), accountKir.getAmount());
    }
}
