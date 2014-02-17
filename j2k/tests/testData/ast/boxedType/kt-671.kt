package demo

open class Test() {
    open fun test() {
        var i: Int? = Integer.valueOf(100)
        var s: Short = 3
        var ss: Short? = java.lang.Short.valueOf(s)
    }
}