class A {
    companion object {
        fun getSimpleName() = this::class.simpleName
    }
}

class B {
    companion object MyCompanion {
        fun getSimpleName() = this::class.simpleName
    }
}

fun getSimpleName(x: Any) = x::class.simpleName
inline fun <reified T> getSimpleNameReified(x: T) = T::class.simpleName

fun box(): String {
    assertEquals("Companion", A.getSimpleName())
    assertEquals("MyCompanion", B.getSimpleName())

    assertEquals("Companion", A.Companion.getSimpleName())
    assertEquals("MyCompanion", B.MyCompanion.getSimpleName())

    assertEquals("Companion", getSimpleName(A.Companion))
    assertEquals("MyCompanion", getSimpleName(B.MyCompanion))

    assertEquals("Companion", getSimpleNameReified(A.Companion))
    assertEquals("MyCompanion", getSimpleNameReified(B.MyCompanion))

    assertEquals("Companion", A.Companion::class.simpleName)
    assertEquals("MyCompanion", B.MyCompanion::class.simpleName)

    return "OK"
}
