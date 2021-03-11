package testing;

import testing.rename.FooKt;

class JavaClient {
    public void foo() {
        String old = FooKt.getFoo();
        FooKt.setFoo(old + "new");
    }
}