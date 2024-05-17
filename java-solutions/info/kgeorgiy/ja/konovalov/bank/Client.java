package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.Bank;
import info.kgeorgiy.ja.konovalov.bank.account.InsufficientFundsException;
import info.kgeorgiy.ja.konovalov.bank.person.Person;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public final class Client {
    
    private static final int DEFAULT_PORT = 239;
    
    private Client() {
    }

    public static void main(final String... args) throws RemoteException {
        if (args == null) {
            System.out.println("args should not be null");
            return;
        }
        if (args.length != 5) {
            System.out.println("Provided args len is incorrect");
            printUsage();
        }
        
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup(String.format("//localhost:%d/bank", DEFAULT_PORT));
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Hardcoded bank URL is invalid");
            return;
        }

        final String name = args[0];
        final String surname = args[1];
        final String passport = args[2];
        final String accountId = args[3];
        final int delta;
        try {
            delta = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.out.println("provided delta should be int");
            printUsage();
            return;
        }
        
        Person person = bank.createPerson(name, surname, passport);
        Account account = person.getAccount(accountId);
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccountForPerson(accountId, person);
        } else {
            System.out.println("Account already exists, good");
        }
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        
        try {
            account.addAmount(delta);
        } catch (InsufficientFundsException e) {
            System.out.println("Could not added delta, since it was negative, and balance was not enough");
        }
        System.out.println("Money: " + account.getAmount());
    }
    
    private static void printUsage() {
        System.out.printf("Usage: <name> <surname> <passport> <account id> <delta in account to add>%n");
    }
}
