package info.kgeorgiy.ja.konovalov.bank.account;

import java.rmi.RemoteException;

public class RemoteAccount implements Account {
    private final String id;
    private int amount;
    
    public RemoteAccount(final String id) {
        this.id = id;
        amount = 0;
    }
    
    public RemoteAccount(final Account other) throws RemoteException {
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
        this.amount = amount;
    }
    
    @Override
    public synchronized void addAmount(final int amount) {
        System.out.println("Adding amount of money for account " + id);
        if (this.amount + amount < 0) {
            throw new InsufficientFundsException(this.amount + amount);
        }
        this.amount += amount;
    }
    
    @Override
    public void transferToAnotherAccount(int amount, Account other) throws RemoteException, InsufficientFundsException, IllegalArgumentException {
        if (other == this) {
            throw new IllegalArgumentException("Transfers to the same account are not allowed");
        }
        
        if (amount < 0) {
            throw new IllegalArgumentException("Could not transfer negative amount ");
        }
        
        addAmount(-amount);
        other.addAmount(amount);
    }
}
