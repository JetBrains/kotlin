package foo

class A() {
    val a = 5
}

inline fun A.myInlineExtension(i: Int) = a + i

fun box() = A().myInlineExtension(1) == 6