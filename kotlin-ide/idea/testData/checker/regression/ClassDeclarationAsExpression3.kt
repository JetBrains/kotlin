fun f(<warning descr="[UNUSED_PARAMETER] Parameter 'i' is never used">i</warning>: Int = 3 < <error descr="[DECLARATION_IN_ILLEGAL_CONTEXT] Declarations are not allowed in this position">class A {
    fun f() {
        f()
    }
}</error>) {

}