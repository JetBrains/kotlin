public interface JavaTest {
    interface FunInterface1 {
        void test();
    }

    interface FunInterface2 {
        int test();
    }

    void foo(FunInterface1 f1);
    void foo(FunInterface2 f2);
}
