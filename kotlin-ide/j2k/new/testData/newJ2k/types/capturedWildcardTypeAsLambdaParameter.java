public class Test {
    public void useSamUpper(SamAcceptor<? extends Number> acceptor) {
        acceptor.acceptSam(p -> {});
    }

    public void useSamLower(SamAcceptor<? super Number> acceptor) {
        acceptor.acceptSam(p -> {});
    }

    public void useSam(SamAcceptor<?> acceptor) {
        acceptor.acceptSam(p -> {});
    }
}