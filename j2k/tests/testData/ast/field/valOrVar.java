class A {
    private final int field1 = 0;
    private int field2 = 0;
    private int field3 = 0;
    final int field4 = 0;
    int field5 = 0;
    private int field6;
    private int field7;
    private int field8;
    private int field9;
    private int field10;
    private int field11;

    A(int p1, int p2, A a) {
        field6 = p1;
        field7 = 10;
        this.field8 = p2;
        this.field9 = 10;
        if (p1 > 0) {
            this.field10 = 10;
        }
        a.field11 = 10;
    }

    void foo() {
        field3 = field2;
    }
}