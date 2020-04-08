public final class Test {
    public static void checkState(boolean condition, String message, Object... args) {
    }

    public static void checkState(boolean condition) {
        checkState(condition, "condition not met");
    }
}
