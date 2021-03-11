package j;

import common.A;

public class Use {
    public static void use(A a) {
        a.id1();
    }

    public static void acceptA(A a) {

    }

    public static A returnA() {
        return new A();
    }
}