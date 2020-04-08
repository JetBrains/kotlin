import java.lang.SuppressWarnings;

class C {
    @Deprecated private final int p1;
    private final int myP2;
    @SuppressWarnings("x") public int p3;

    public C(int p1, @Deprecated int p2, @Deprecated int p3) {
        this.p1 = p1;
        myP2 = p2;
        this.p3 = p3;
    }
}
