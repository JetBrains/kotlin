package bar;

import foo.*;

public class BarJavaClass {
    void f() {
        new BarKotlinClass();
        new FooJavaClass();
        new FooKotlinClass();
    }
}