// RUNTIME_WITH_FULL_JDK

// we intentionally do not convert interface to Kotlin fun interface
// if it inherits from some other interface, because it is hard to deal
// with default methods which were already converted to properties
// (and in kotlin fun interface cannot have abstract property)

public interface MyRunnableBase {
    default int getValue() {
        return 0;
    }
}

@FunctionalInterface
public interface MyRunnable extends MyRunnableBase {
    @Override
    int getValue();
}