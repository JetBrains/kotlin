package foo;

import bar.*;

public class FooJavaClass {
    void f() {
        new FooKotlinClass();
        new BarJavaClass();
        new BarKotlinClass();
    }
}