internal class A {
    fun foo(s: String?): Int {
        return s?.length ?: -1
    }
}