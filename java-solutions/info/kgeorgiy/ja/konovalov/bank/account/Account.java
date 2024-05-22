package info.kgeorgiy.ja.konovalov.bank.account;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    
    /**
     * Returns account identifier.
     */
    String getId() throws RemoteException;
    
    /**
     * Returns amount of money in the account.
     */
    int getAmount() throws RemoteException;
    
    
    /**
     * Sets amount of money in the account.
     */
    void setAmount(int amount) throws RemoteException, InsufficientFundsException;
    
    /**
     * Adds amount of money in the account.
     */
    void addAmount(int amount) throws RemoteException, InsufficientFundsException;
}
