package test

object Utils {
    @JvmStatic
    fun foo1(p: Int) {}
    @JvmStatic
    fun foo2(): Int {
        return 1
    }

    @JvmField
    var staticField = 1
}