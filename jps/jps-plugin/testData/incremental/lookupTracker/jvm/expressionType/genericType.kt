package foo

// From KT-10772 Problem with daemon on Idea 15.0.3 & 1-dev-25

/*p:foo(identity) p:kotlin(Suppress)*/@/*p:foo p:kotlin*/Suppress("CAST_NEVER_SUCCEEDS_ERROR")
fun <T> identity(): /*p:kotlin(Function1)*/(/*p:foo*/T) -> /*p:foo*/T = null as /*p:kotlin(Function1)*/(/*p:foo*/T) -> /*p:foo*/T

/*p:foo*/fun <T> compute(f: /*p:kotlin(Function0)*/() -> /*p:foo*/T) {
    val result = /*p:kotlin(Function0) p:kotlin.Function0(invoke)*/f()
}

/*p:foo*/class Bar<T>(val t: /*p:foo*/T) {
    init {
        val a = /*p:foo.Bar*/t
    }
}
