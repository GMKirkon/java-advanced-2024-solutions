package info.kgeorgiy.ja.konovalov.bank.account;

// :NOTE: с большой буквы
public class tooMuchMoneyException extends RuntimeException {
    public tooMuchMoneyException(int currentvalue, int added) {
        super(String.format(
                "Impossible to proceed the transaction,"
                + " do not support more than INT_MAX balance, current balance: %d, attempt to add %d",
                currentvalue,
                added
        ));
    }
}
