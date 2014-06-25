//file
class A {
    private int v;

    // this is a primary constructor
    A(int p) {
        v = 1;
    } // end of primary constructor body

    // this is a secondary constructor
    A() {
        this(1);
    } // end of secondary constructor body
}

class B {
    private int x;

    // this constructor will disappear
    B(int x) {
        this.x = x;
    } // end of constructor body

    void foo(){}
}