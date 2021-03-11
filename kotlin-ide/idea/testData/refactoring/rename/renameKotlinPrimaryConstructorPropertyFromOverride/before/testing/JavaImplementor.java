package testng;

import testing.rename.Foo;

public class FooImpl extends Foo {
    public FooImpl(String first) {
        super(first);
    }

    @Override
    public int getFirst() {
        return "abc";
    }
}