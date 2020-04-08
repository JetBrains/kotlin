package usages;

import target.Foo;
import target.TestKt;

class Test {
    static void test() {
        new Foo();
        TestKt.foo();
    }
}