package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.person.ILocalPerson;
import info.kgeorgiy.ja.konovalov.bank.person.IRemotePerson;
import info.kgeorgiy.ja.konovalov.bank.person.LocalPerson;
import info.kgeorgiy.ja.konovalov.bank.person.Person;
import info.kgeorgiy.ja.konovalov.bank.person.RemotePerson;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it does not already exist.
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id) throws RemoteException;
    
    
    /**
     * Creates new person, as RemotePerson
     * @param name persons name
     * @param surname persons surname
     * @param passportNumber persons passport
     * @return created person with no accounts
     */
    IRemotePerson createPerson(String name, String surname, String passportNumber) throws RemoteException;
    

    /**
     * Returns account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exist.
     */
    Account getAccount(String id) throws RemoteException;
    
    /**
     * Returns person by passport Number
     * @param passportNumber number to search by
     * @return remote instance of found person of {@code null} if such person does not exist
     */
    ILocalPerson getLocalPerson(String passportNumber) throws RemoteException;
    
    /**
     * Returns person by passport Number
     * @param passportNumber number to search by
     * @return local instance of found person of {@code null} if such person does not exist
     */
    IRemotePerson getRemotePerson(String passportNumber) throws RemoteException;
    
    /**
     * Creates account with given id for person
     * @param id accounts id
     * @param person person for whom the account is being created
     * @return created account for person
     */
    Account createAccountForPerson(final String id, final Person person) throws RemoteException;
}
