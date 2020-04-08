class Foo <fold text='{...}' expand='true'>{
    <fold text='Methods' expand='true'>// <editor-fold desc="Methods">
    fun foo() {}

    fun bar() <fold text='{...}' expand='true'>{

    }</fold>
    // </editor-fold></fold>

    fun xyzzy() <fold text='{...}' expand='true'>{
        <fold text='Body' expand='true'>// <editor-fold desc="Body">
        printn("Xyzzy")
        printn("Xyzzy")
        printn("Xyzzy")
        // </editor-fold></fold>
    }</fold>
}</fold>

