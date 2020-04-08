package foo;

class A {
    void/* nothing to return */ foo(/* no parameters at all */) {
        // let declare a variable
        // with 2 comments before
        int/*int*/ a /* it's a */ = 2 /* it's 2 */ + 1 /* it's 1 */; // variable a declared
    } // end of foo

    int/* we return int*/ foo(int/*int*/ p/* parameter p */) { /* body is empty */ }

    private/*it's private*/ int field = 0;

    public /*it's public*/ char foo(String s) { }

    protected/*it's protected*/ void foo(char c) { }

    /**
     * Method description.
     * Multi-line method description.
     *
     *
     * @param param1 param1 description
     * @param param2 param2 description
     *
     * @param param3 param3 description
     */
    public void foo(String param1, String param2, String param3) {}

    public/*it's public*/ static/*and static*/ final/*and final*/ int C = 1;
}