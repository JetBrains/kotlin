public class TestLongOfTwoInts {
    public long foo(int x1, int x2) {
        return x1 | ((long) x2 << 32);
    }
}