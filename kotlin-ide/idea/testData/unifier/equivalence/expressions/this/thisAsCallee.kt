// DISABLE-ERRORS
fun (String.() -> String).foo(x : String) {
    x.<selection>this</selection>()
    x.this()
    x.this(1)
    x.this@foo()
    x.this@foo(2)
}