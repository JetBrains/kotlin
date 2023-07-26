package foo

// From KT-10772 Problem with daemon on Idea 15.0.3 & 1-dev-25

/*p:foo*/fun <T> identity(): (/*p:foo*/T) -> /*p:foo*/T = null as (/*p:foo*/T) -> /*p:foo*/T

/*p:foo*/fun <T> compute(f: () -> /*p:foo*/T) {
    val result = /*p:Function0(invoke) p:kotlin(Function0) p:kotlin.Function0<T>(invoke)*/f()
}

/*p:foo*/class Bar<T>(val t: /*p:foo*/T) {
    init {
        val a = /*p:Bar p:foo.Bar<T>*/t
    }
}
