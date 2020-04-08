class A {
    private int v;

    // this is a primary constructor
    A(int p) {
        v = 1;
    } // end of primary constructor body

    // this is a secondary constructor 1
    A() {
        this(1);
    } // end of secondary constructor 1 body

    // this is a secondary constructor 2
    A(String s) {
        this(s.length());
    } // end of secondary constructor 2 body
}

class B {
    private int x;

    // this constructor will disappear
    public B(int x) {
        this.x = x;
    } // end of constructor body

    void foo(){}
}

class CtorComment {
    public String myA;

    /*
     * The magic of comments
     */
    // single line magic comments
    public CtorComment() {
        myA = "a";
    }
}

class CtorComment2 {
    /*
     * The magic of comments
     */
    // single line magic comments
    public CtorComment2() {}
}