package simple

interface I {
    fun interfaceFun(default: Int = 42)

    companion object {
        val companionObjectVal = "foo"
    }
}

fun <T> take(x: T) { }
fun getBoolean(): Boolean = true

fun functionCalls(i: I) {
    if (getBoolean()) {
        take(I.companionObjectVal)
    }

    while (getBoolean()) {
        i.interfaceFun()
    }
}
