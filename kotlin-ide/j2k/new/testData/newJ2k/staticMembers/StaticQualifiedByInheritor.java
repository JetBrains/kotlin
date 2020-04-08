public class Base {
    public static void foo() {
    }
}

public class Derived extends Base {
}

public class User {
    public static void test() {
        Derived.foo();
    }
}