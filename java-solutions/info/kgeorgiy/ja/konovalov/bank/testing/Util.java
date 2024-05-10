package info.kgeorgiy.ja.konovalov.bank.testing;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class Util {
    
    private Util() {
    }
    
    public static void server(String prefix, int threads, double p, DatagramSocket socket) {
//        return () -> {
//            int[] expected = new int[threads];
//            Random random = new Random(239239239239239239L + (long) Objects.hash(new Object[]{prefix, threads, p}));
//
//            try {
//                while(true) {
//                    DatagramPacket packet = createPacket(socket);
//                    socket.receive(packet);
//                    String request = getString(packet);
//                    String message = "Invalid or unexpected request " + request;
//                    Assertions.assertTrue(request.startsWith(prefix), message);
//                    String[] parts = request.substring(prefix.length()).split("_");
//                    Assertions.assertEquals(2, parts.length, message);
//
//                    try {
//                        int thread = Integer.parseInt(parts[0]) - 1;
//                        int no = Integer.parseInt(parts[1]);
//                        Assertions.assertTrue(0 <= thread && thread < expected.length, message);
//                        Assertions.assertEquals(expected[thread] + 1, no, message);
//                        String response = mode.response(request, random);
//                        if (no != 0 && !(p >= random.nextDouble())) {
//                            if (random.nextBoolean()) {
//                                send(socket, packet, mode.corrupt(response, random));
//                            }
//                        } else {
//                            int var10002 = expected[thread]++;
//                            send(socket, packet, response);
//                        }
//                    } catch (NumberFormatException var14) {
//                        throw new AssertionError(message);
//                    }
//                }
//            } catch (IOException var15) {
//                if (socket.isClosed()) {
//                    return expected;
//                } else {
//                    throw var15;
//                }
//            }
//        };
    }
    
    private static <T> T select(List<T> items, Random random) {
        return items.get(random.nextInt(items.size()));
    }
}