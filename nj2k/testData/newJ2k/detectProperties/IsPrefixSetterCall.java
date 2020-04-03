public class TestSetter {
    private static void setThing(int thing) { }

    private static int isThing() { return 42; }

    public static void main(String[] args) {
        setThing(42);
        System.out.println(isThing());
    }
}