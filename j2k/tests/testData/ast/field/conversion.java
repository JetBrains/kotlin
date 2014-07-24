//file
class A {
    private Integer i = getByte();

    static byte getByte() { return 0; }

    void foo() {
        i = 10;
    }
}