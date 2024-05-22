package info.kgeorgiy.ja.konovalov.bank.person;

import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.account.IRemoteAccount;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Person extends Remote {
    String getName() throws RemoteException;
    
    String getSurname() throws RemoteException;
    
    String getPassportNumber() throws RemoteException;
    
    IRemoteAccount addAccount(String fullId) throws RemoteException;
    
    Map<String, Account> getAllAccounts() throws RemoteException;
    
    Account getAccount(String accountBankId) throws RemoteException;
}

