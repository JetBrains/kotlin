package demo

open class Test() {
    open fun putInt(i: Int?) {
    }
    open fun test() {
        var b: Byte = 10
        putInt(b.toInt())

        var b2: Byte? = 10
        putInt(b2?.toInt())
    }
}