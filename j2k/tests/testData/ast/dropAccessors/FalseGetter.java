public class AAA {
    private int x = 42;
    private AAA other = new AAA();

    public int getX() {
        return other.x;
    }
}
