package info.kgeorgiy.ja.konovalov.bank.account;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(int value) {
        super(String.format("Impossible to proceed the transaction, the balance would become: %d", value));
    }
}
