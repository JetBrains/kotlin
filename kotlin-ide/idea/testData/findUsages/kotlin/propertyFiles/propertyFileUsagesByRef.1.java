import org.jetbrains.annotations.PropertyKey;

class A {
    static String message(@PropertyKey(resourceBundle = "propertyFileUsagesByRef.2") String key, Object... args) {
        return key;
    }

    void test() {
        message("foo.bar");
    }
}