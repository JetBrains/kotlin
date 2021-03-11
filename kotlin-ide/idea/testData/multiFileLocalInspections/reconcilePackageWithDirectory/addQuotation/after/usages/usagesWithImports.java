package usages;

import in.foo.fun.Foo;
import static in.foo.fun.TestKt.foo;

class Test {
    static void test() {
        new Foo();
        foo();
    }
}