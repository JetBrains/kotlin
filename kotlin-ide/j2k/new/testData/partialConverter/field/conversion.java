class A {
    private Integer <caret>i = getByte();

    static byte getByte() { return 0; }

    void foo() {
        i = 10;
    }
}
