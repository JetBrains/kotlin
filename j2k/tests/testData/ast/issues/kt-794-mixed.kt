package demo

open class Test() {
    open fun getInteger(i: Int?): Int? {
        return i
    }

    open fun test() {
        var i: Int = getInteger(10)!!
    }
}