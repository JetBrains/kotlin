package foo

// From KT-10772 Problem with daemon on Idea 15.0.3 & 1-dev-25

/*p:foo*/fun <T> identity(): (T) -> T = /*p:kotlin(Nothing) p:kotlin(Function1)*/null as (T) -> T

/*p:foo*/fun <T> compute(f: () -> T) {
    val result = f()
}

/*p:foo*/class Bar<T>(val t: T) {
    init {
        val a = /*c:foo.Bar c:foo.Bar(T)*/t
    }
}