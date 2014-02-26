package foo

class A() {
    val a = 5
}

inline fun A.myInlineExtension() = a + 1

fun box() = A().myInlineExtension() == 6