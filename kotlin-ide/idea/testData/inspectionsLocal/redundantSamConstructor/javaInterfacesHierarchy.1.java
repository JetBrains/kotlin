interface Base {
    void test1();
}

interface Extender extends Base {
    @java.lang.Override
    default void test1() { test2(); }

    void test2();
}

class Taker {
    static void take(Base b) {}
}