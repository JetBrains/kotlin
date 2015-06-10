class A {
    jvmOverloads fun foo(i: Int, c: Char = 'a', s: String = "") {
        println("foo$i$c$s")
    }

    jvmOverloads fun bar(s: String? = null): Int {
        println("s = " + s!!)
        return 0
    }
}
