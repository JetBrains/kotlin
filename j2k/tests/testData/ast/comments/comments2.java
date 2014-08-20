package foo;

class A {
    void/* nothing to return */ foo(/* no parameters at all */) {
        // let declare a variable
        // with 2 comments before
        int/*int*/ a /* it's a */ = 2 /* it's 2 */ + 1 /* it's 1 */; // variable a declared
    } // end of foo

    int/* we return int*/ foo(int/*int*/ p/* parameter p */) { /* body is empty */ }

    private/*it's private*/ int field = 0;

    public /*it's public*/ char foo() { }

    protected/*it's protected*/ void foo() { }

    public/*it's public*/ static/*and static*/ final/*and final*/ int C = 1;
}