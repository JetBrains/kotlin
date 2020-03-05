interface Foo {
}

public class Bar {
    void test() {
        new Foo() {
            void foo() {
                bug(this);
            }
        };
    }

    void bug(Foo foo) {
    }
}