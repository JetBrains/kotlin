package test;

public abstract class Base {
    protected int field;

    public Base(int value) {
        field = value;
    }
}

public class Derived extends Base {
    public Derived(int value) {
        super(value);
    }

    private View usage = new View() {
        @Override
        void click() {
            int activity = field;
        }
    }
}

abstract class View {
    abstract void click();
}
