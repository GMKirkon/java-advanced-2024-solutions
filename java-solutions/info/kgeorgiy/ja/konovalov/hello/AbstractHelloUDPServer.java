package info.kgeorgiy.ja.konovalov.hello;

import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AbstractHelloUDPServer implements NewHelloServer {
    static protected final Charset SERVER_CHARSET = StandardCharsets.UTF_8;
    static protected final int HANDLER_BUFFER_SIZE = 1024;
    protected final ConcurrentLinkedQueue<Exception> receivingExceptions = new ConcurrentLinkedQueue<>();
    protected final ConcurrentLinkedQueue<Exception> sendingExceptions = new ConcurrentLinkedQueue<>();
    protected final Timer timer;
    protected ExecutorService queryHandlersPool;
    protected volatile serverStates currentState = serverStates.NOT_STARTED;
    
    /**
     * enum for server states, by contract server could only be run once
     */
    protected enum serverStates {
        NOT_STARTED,
        WORKING,
        DOING_NOTHING_EMPTY,
        CLOSING
    }
    
    protected void chengeServerStatusDueToStart(int threads) {
        Internal.checkForPositive(threads, "cannot listen on nonpositive number of threads", true);
        
        switch (currentState) {
            case WORKING, DOING_NOTHING_EMPTY -> throw new IllegalStateException("Server is already running");
            case CLOSING -> throw new IllegalStateException("Server already finished working and could not be restarted");
        }
    }
    
    /**
     * creates Server that would be closed after timeout, note that timeout is in int,
     * so server could be working upto ~63 YEARS
     *
     * @param timeoutInSeconds time until executing close()
     */
    public AbstractHelloUDPServer(int timeoutInSeconds) {
        timer = new Timer();
        timer.schedule(new SelfCloseTask(), timeoutInSeconds * 1000L);
    }
    
    /**
     * Creates Server without timeout
     */
    public AbstractHelloUDPServer() {
        timer = null;
    }
    
    /**
     * TimerTask to close Server after some timeout
     */
    private final class SelfCloseTask extends TimerTask {
        @Override
        public void run() {
            close();
        }
    }
    
    abstract protected AbstractHelloUDPServer getServer();
    abstract protected AbstractHelloUDPServer getServer(int timeout);
    
    protected void mainImpl(String... args) {
        if (args == null) {
            System.out.println("Provided args should not be null");
            printUsage();
            return;
        }
        
        if (args.length != 2) {
            printUsage();
        }
        
        Integer port = Internal.parsePositiveInteger(args[0], "port ");
        Integer threads = Internal.parsePositiveInteger(args[1], "number of threads ");
        if (port == null || threads == null) {
            return;
        }
        
        var server = getServer();
        server.start(port, threads);
    }
    
    private static void printUsage() {
        System.out.println("Usage: <port> <number of handlers>");
    }
    
    /**
     * Returns list of exceptions happened during receiving data, if server is still running returns null
     *
     * @return List of receiving exceptions that happened during last server session
     */
    public List<Exception> getReceivingExceptions() {
        return getExceptions(receivingExceptions);
    }
    
    private List<Exception> getExceptions(Collection<Exception> exceptions) {
        if (currentState != serverStates.CLOSING) {
            return null;
        }
        return exceptions.stream().toList();
    }
    
    /**
     * Returns list of exceptions happened during sending data, if server is still running returns null
     *
     * @return List of sending exceptions that happened during last server session
     */
    public List<Exception> getSendingExceptions() {
        return getExceptions(sendingExceptions);
    }
    
    protected void startHelper(int threads, Map<Integer, String> ports) {
        chengeServerStatusDueToStart(threads);
        
        if (ports.isEmpty()) {
            currentState = serverStates.DOING_NOTHING_EMPTY;
            return;
        }
        
        currentState = serverStates.WORKING;
        queryHandlersPool = Executors.newFixedThreadPool(threads);
    }
    
    protected boolean closeHelper() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        
        if (currentState == serverStates.DOING_NOTHING_EMPTY) {
            currentState = serverStates.CLOSING;
            return true;
        }
        
        queryHandlersPool.shutdown();
        return false;
    }
    
    abstract protected void closeAllRunningThreads();
    
    @Override
    public void close() {
        if (closeHelper()) {
            return;
        }
        
        closeAllRunningThreads();
        currentState = serverStates.NOT_STARTED;
    }
}
