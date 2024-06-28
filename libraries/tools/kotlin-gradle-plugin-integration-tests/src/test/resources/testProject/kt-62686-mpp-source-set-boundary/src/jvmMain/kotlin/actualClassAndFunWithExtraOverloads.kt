class jvmMainOnlyType

actual class A {
    actual fun foo(x: Base) = "aloha"
    fun foo(x: Child) = jvmMainOnlyType()
}

fun foo(x: Child) = jvmMainOnlyType()

fun testPlatform() =
    A().foo(Child())

fun main() {
    println(test())
    println(testPlatform())
}

private val dependency = dependedOnByActualDeclaration
