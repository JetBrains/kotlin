package test;

public class TestIntCompatibleAsArrayIndex {
    private byte b;
    private short s;
    private int[] ints = new int[4];

    public void foo(int i) {
        ints[b] = i;
        ints[s] = i;
    }
}