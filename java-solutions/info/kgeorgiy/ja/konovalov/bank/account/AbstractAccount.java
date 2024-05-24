package info.kgeorgiy.ja.konovalov.bank.account;

import java.rmi.RemoteException;

public class AbstractAccount implements Account {
    
    private final String id;
    private int amount;
    
    public AbstractAccount(final String id) {
        this.id = id;
        amount = 0;
    }
    
    public AbstractAccount(final Account other) throws RemoteException {
        synchronized (other) {
            this.id = other.getId();
            amount = other.getAmount();
        }
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }
    
    @Override
    public synchronized void setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + id);
        if (amount < 0) {
            throw new IllegalArgumentException("could not set negative amount");
        }
        this.amount = amount;
    }

    @Override
    public synchronized void addAmount(final int added) {
        System.out.println("Adding amount of money for account " + id);
        if (added < 0 && this.amount + added < 0) {
            throw new InsufficientFundsException(this.amount + added);
        }
        if (added > 0 &&  Integer.MAX_VALUE - added < this.amount) {
            throw new TooMuchMoneyException(this.amount, added);
        }
        this.amount += added;
    }
}
