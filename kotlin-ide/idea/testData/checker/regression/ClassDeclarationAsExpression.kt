val p = 1 < <error descr="[DECLARATION_IN_ILLEGAL_CONTEXT] Declarations are not allowed in this position">class A {
    fun f() {
        f()
    }
}</error>
