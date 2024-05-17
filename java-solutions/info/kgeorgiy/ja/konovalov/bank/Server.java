package info.kgeorgiy.ja.konovalov.bank;

import info.kgeorgiy.ja.konovalov.bank.Bank;
import info.kgeorgiy.ja.konovalov.bank.RemoteBank;
import info.kgeorgiy.ja.konovalov.bank.RmiAccountsPolicy;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public final class Server {
    private final static int DEFAULT_PORT = 239;
    
    private Server() {
    
    }
    
    public static void main(final String... args) {
        if (args == null) {
            printUsage();
            return;
        }
        
        try {
            final int bankPort = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
            if (bankPort < 0 || bankPort > 65535) {
                System.out.println("Port should be valid, between 0 and 65355");
                printUsage();
                return;
            }
            
            final int policyInt = args.length > 1 ? Integer.parseInt(args[1]) : 1;
            final RmiAccountsPolicy policy;
            if (policyInt < 1 || policyInt > 4) {
                printUsage();
                return;
            } else {
                policy = RmiAccountsPolicy.values()[policyInt - 1];
            }
            
            final Bank bank = new RemoteBank(List.of(DEFAULT_PORT), policy);
            try {
                if (LocateRegistry.getRegistry(bankPort) != null) {
                    LocateRegistry.createRegistry(bankPort);
                }
                UnicastRemoteObject.exportObject(bank, bankPort);
                Naming.rebind(String.format("//localhost:%d/bank", bankPort), bank);
                System.out.println("Server started");
            } catch (final RemoteException e) {
                System.out.println("Cannot export object: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            } catch (final MalformedURLException e) {
                System.out.println("Malformed URL");
            }
        } catch (NumberFormatException e) {
            printUsage();
        }
    }
    
    public static void printUsage() {
        System.out.println("Usage:%n <server rmi port> <scheduling accounts and persons ports policy>");
        System.out.println("for single port use 1 as scheduling policy%n "
                           + "for multiple by chunks use 2%n"
                           + "for random port from list of default ports use 3%n"
                           + "for load balancing over list of default ports use 4%n");
    }
}
