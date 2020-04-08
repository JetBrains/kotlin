package usages;

import target.Foo;
import static target.TestKt.foo;

class Test {
    static void test() {
        new Foo();
        foo();
    }
}