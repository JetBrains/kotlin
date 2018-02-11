class Outer {
    private inner class Inner(val foo: String, val bar: String)
    private class Nested(val foo: String, val bar: String)

    fun nonAbstract(s: String, i: Int) {

    }

    abstract fun abstract(s: String, i: Int)
}