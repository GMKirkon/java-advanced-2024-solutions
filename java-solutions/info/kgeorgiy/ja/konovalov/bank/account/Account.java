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
     * Sets amount of money in the account.
     */
    void addAmount(int amount) throws RemoteException, InsufficientFundsException;
    
    /**
     * Does the transaction between accounts
     *
     * @param amount amount to transfer from the account
     * @param other  account where to transfer the money
     * @throws InsufficientFundsException in case not enough money on the account
     * @throws IllegalArgumentException   if you try to transfer negative amount of money
     */
    void transferToAnotherAccount(int amount, Account other) throws RemoteException, InsufficientFundsException, IllegalArgumentException;
}
