package foo

class A() {
    fun f(): Boolean {
        object t {
            val c = true;
        }
        val z = object {
            val c = true
        }
        return t.c && z.c
    }
}

fun box() = A().f();
