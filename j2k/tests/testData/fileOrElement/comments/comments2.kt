package foo

class A {
    fun /* nothing to return */ foo(/* no parameters at all */) {
        // let declare a variable
        // with 2 comments before
        val /*int*/ a /* it's a */ = 2 /* it's 2 */ + 1 /* it's 1 */ // variable a declared
    } // end of foo

    fun /* we return int*/ foo(/*int*/ p: Int/* parameter p */): Int {
        /* body is empty */
    }

    private /*it's private*/ val field = 0

    public /*it's public*/ fun foo(): Char {
    }

    protected /*it's protected*/ fun foo() {
    }

    class object {

        public /*it's public*//*and static*//*and final*/ val C: Int = 1
    }
}