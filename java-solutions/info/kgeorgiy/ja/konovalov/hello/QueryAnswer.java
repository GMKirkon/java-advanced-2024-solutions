package info.kgeorgiy.ja.konovalov.hello;

import java.net.SocketAddress;

public record QueryAnswer(String answer, SocketAddress address) {
}
