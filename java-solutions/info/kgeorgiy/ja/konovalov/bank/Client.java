package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.account.Account;
import info.kgeorgiy.ja.konovalov.bank.Bank;
import info.kgeorgiy.ja.konovalov.bank.account.InsufficientFundsException;
import info.kgeorgiy.ja.konovalov.bank.person.Person;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import static info.kgeorgiy.ja.konovalov.bank.Util.throwOrSuppress;

public final class Client {
    
    private static final int DEFAULT_PORT = 239;
    private static final String USAGE_MESSAGE = "Usage: <name> <surname> <passport> <account id> <delta in account to add>%n";
    
    private Client() {
    }
    
    private static void actualMain(boolean doesThrow, final String... args) {
        if (args == null) {
            throwOrSuppress(doesThrow, new RuntimeException(USAGE_MESSAGE), "");
            printUsage();
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
            throwOrSuppress(doesThrow, e, "Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            throwOrSuppress(doesThrow, e, "Hardcoded bank URL is invalid");
            return;
        } catch (RemoteException e) {
            throwOrSuppress(doesThrow, e, "Fix network, currently does not work");
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
            throwOrSuppress(doesThrow, e, "provided delta should be int");
            printUsage();
            return;
        }
        
        try {
            Person person = bank.createPerson(name, surname, passport);
            Account account = person.getAccount(accountId);
            if (account == null) {
                System.out.println("Creating account");
                try {
                    account = person.addAccount(accountId);
                } catch (RemoteException e) {
                    throwOrSuppress(doesThrow, e, "Could not create account for person with error: ");
                }
            } else {
                System.out.println("Account already exists, good");
            }
            System.out.println("Account id: " + account.getId());
            System.out.println("Money: " + account.getAmount());
            System.out.println("Adding money");
            
            try {
                account.addAmount(delta);
            } catch (InsufficientFundsException e) {
                throwOrSuppress(doesThrow, e, "Could not added delta, since it was negative, and balance was not enough");
            } catch (RemoteException e) {
                throwOrSuppress(doesThrow, e, "Could not add amount to the account ");
            }
            System.out.println("Money: " + account.getAmount());
        } catch (RemoteException e) {
            throwOrSuppress(doesThrow, e, "Could not get person from bank with error: ");
        }
    }
    
    

    public static void main(final String... args) {
        actualMain(false, args);
    }
    
    private static void printUsage() {
        System.out.printf(USAGE_MESSAGE);
    }
    /* package-private */
    static void testingMain(final String... args) {
        actualMain(true, args);
    }
}
