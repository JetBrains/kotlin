public class AAA {
    private int x = 42;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void foo() {
        setX(getX() + 1);
    }

    public void bar(AAA other) {
        other.setX(other.getX() + 1);
    }
}

class B {
    void foo(AAA a) {
        a.setX(a.getX() + 1);
    }
}
