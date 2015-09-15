internal class A {
    @JvmOverloads internal fun foo(i: Int, c: Char = 'a', s: String = "") {
        println("foo" + i + c + s)
    }

    @JvmOverloads internal fun bar(s: String? = null): Int {
        println("s = " + s!!)
        return 0
    }
}
