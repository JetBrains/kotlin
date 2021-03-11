package j2;

import common.A;

public class Use {
    public static void use() {
        A a = new A();
        a.id2();
    }

    public static void acceptA(A a) {

    }

    public static A returnA() {
        return new A();
    }
}