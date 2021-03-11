package test.hierarchy;

public class Base {
    public void doStuff(String s) {}
}

public class Derived extends Base {
    @Override
    public void doStuff(String s) {
        if (s == null)
            System.out.println("null");
        else
            System.out.println("not null: " + s);
    }
}