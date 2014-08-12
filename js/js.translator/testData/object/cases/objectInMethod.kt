package foo

class A() {
    fun f(): Boolean {
        val z = object {
            val c = true
        }
        return z.c
    }
}

fun box() = A().f();
