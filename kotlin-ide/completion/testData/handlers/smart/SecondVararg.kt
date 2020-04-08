fun foo(vararg v1: Int, vararg v2: Char, s: String) { }

fun bar(i: Int) {
    foo(*intArrayOf(), <caret>)
}

// ELEMENT: i
