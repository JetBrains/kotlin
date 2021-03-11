public class AAA {
    protected int myX = 42;

    public int getX() {
        return myX;
    }

    public void foo(AAA other) {
        System.out.println(myX);
        System.out.println(other.myX);
        myX = 10;
    }
}

class BBB extends AAA {
    void bar() {
        System.out.println(myX);
        myX = 10;
    }
}
