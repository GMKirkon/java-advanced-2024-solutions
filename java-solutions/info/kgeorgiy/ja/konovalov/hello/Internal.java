package info.kgeorgiy.ja.konovalov.hello;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Internal {
    static Integer parsePositiveInteger(String arg, String errorMessage) {
        try {
            var result = Integer.parseInt(arg);
            if (result < 0) {
                System.err.printf("%s should be positive", errorMessage);
            }
            return result;
        } catch (NumberFormatException e) {
            System.err.printf("%s should be integer", errorMessage);
            return null;
        }
    }
    
    static boolean checkForPositive(int num, String errorMessage, boolean doesThrow) {
        if (num < 0) {
            if (doesThrow) {
                throw new IllegalStateException(errorMessage);
            } else {
                System.err.println(errorMessage);
                return false;
            }
        } else {
            return true;
        }
    }
    
    
    static Consumer<Exception> suppressingConsumer(Exception[] singleException) {
        return (Exception e) -> {
            if (singleException[0] == null) {
                singleException[0] = e;
            } else {
                singleException[0].addSuppressed(e);
            }
        };
    }
    
    static void unwrapSuppressedException(Exception[] singleException, boolean doesThrow) {
        if (singleException[0] != null) {
            if (doesThrow) {
                throw new RuntimeException(singleException[0]);
            } else {
                System.out.println(singleException[0].getMessage());
            }
        }
    }
    
    // code should be in some class across all homeworks starting from 7, however, as I know, GK compiling pipeline
    // does not allow to use class from different packages, so gonna double it here
    static void closeThread(Thread thread) {
        thread.interrupt();
        boolean joined = false;
        while (!joined) {
            try {
                thread.join();
                joined = true;
            } catch (InterruptedException ignored) {
            }
        }
    }
    
    static void performOperationWithExceptionConsumer(Runnable run, Consumer<Exception> suppressingConsumer) {
        try {
            run.run();
        } catch (RuntimeException e) {
            suppressingConsumer.accept(e);
        }
    }
    
    static void closeWithExceptionConsumer(AutoCloseable element, Consumer<Exception> suppressingConsumer) {
        try {
            element.close();
        } catch (Exception e) {
            suppressingConsumer.accept(e);
        }
    }
    
    static void closeDatagramChannels(List<? extends AutoCloseable> datagramChannels, Consumer<Exception> suppressingConsumer) {
        datagramChannels.forEach(x -> closeWithExceptionConsumer(x, suppressingConsumer));
    }
    
    static DatagramChannel addDatagramChannel(List<DatagramChannel> datagramChannels) throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        datagramChannels.add(channel);
        return channel;
    }
    
    static void performSelectingOperation(final Selector selector, final Consumer<Exception> addSuppressedException, final BiConsumer<SelectionKey, DatagramChannel> readConsumer, final BiConsumer<SelectionKey, DatagramChannel> writeConsumer, final int SELECTOR_TIMEOUT, final Runnable actionForSelectingZero) {
        while (!Thread.currentThread().isInterrupted() && !selector.keys().isEmpty()) {
            try {
                if (selector.select(key -> Internal.performOperationWithExceptionConsumer(() -> {
                    final var channel = (DatagramChannel) key.channel();
                    if (key.isReadable()) {
                        readConsumer.accept(key, channel);
                    }
                    
                    if (key.isWritable()) {
                        writeConsumer.accept(key, channel);
                    }
                }, addSuppressedException), SELECTOR_TIMEOUT) == 0) {
                    actionForSelectingZero.run();
                }
            } catch (IOException e) {
                addSuppressedException.accept(e);
            }
        }
    }
}
