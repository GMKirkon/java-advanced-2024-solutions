/**
 * homework solutions module
 * for <a href="https://www.kgeorgiy.info/courses/java-advanced/">Java Advanced</a> course.
 *
 * @author Kirill Konovalov (367307@edu.itmo.ru)
 */
module info.kgeorgiy.ja.konovalov {
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.iterative;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;
    
    requires java.compiler;
    requires java.rmi;
    requires jdk.httpserver;
    requires org.junit.platform.engine;
    requires org.junit.platform.launcher;
    
    requires transitive org.junit.jupiter.api;
    
    exports info.kgeorgiy.ja.konovalov.bank;
    opens info.kgeorgiy.ja.konovalov.bank to org.junit.platform.launcher;
    
    exports info.kgeorgiy.ja.konovalov.bank.account to java.rmi, org.junit.platform.commons;
    exports info.kgeorgiy.ja.konovalov.bank.person to java.rmi, org.junit.platform.commons;
}