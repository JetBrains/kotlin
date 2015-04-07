public class AAA {
    public void foo() {
        setX(getX() + 1);
    }

    private static int ourX = 42;

    public static int getX() {
        return ourX;
    }

    public static void setX(int x) {
        ourX = x;
    }
}
