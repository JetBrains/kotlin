// RUNTIME_WITH_FULL_JDK

@FunctionalInterface
public interface MyRunnable {
    int getResult();

    default int getDoubleResult() {
        return getResult() * 2;
    }
}