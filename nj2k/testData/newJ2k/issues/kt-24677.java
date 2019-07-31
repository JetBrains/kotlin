import java.util.ArrayList;

interface FooInterface {
    ArrayList<? extends Foo.SomeClass> foo();
}

public class Foo implements FooInterface {

    @Override
    public ArrayList<SomeClass> foo() {
        return null;
    }

    public static class SomeClass {
    }
}