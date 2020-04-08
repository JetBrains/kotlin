public class AAA {
    private int myX = 42;

    public int getX() {
        return myX;
    }

    public void foo(AAA other) {
        System.out.println(myX);
        System.out.println(other.myX);
        myX = 10;
    }
}
