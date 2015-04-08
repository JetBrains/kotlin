class A {
    overloads fun foo(i: Int, c: Char = 'a', s: String = "") {
        println("foo" + i + c + s)
    }

    overloads fun bar(s: String? = null): Int {
        println("s = " + s!!)
        return 0
    }
}
