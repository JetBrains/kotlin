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

    public void bar(B b) {
        System.out.println(b.getYY());
    }
}
