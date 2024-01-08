class jsMainOnlyType()

actual class A {
    actual fun foo(x: Base) = "aloha"
    fun foo(x: Child) = jsMainOnlyType()
}

fun foo(x: Child) = jsMainOnlyType()

fun testPlatform() =
    A().foo(Child())

fun main() {
    println(test())
    println(testPlatform())
}

private val dependency = dependedOnByActualDeclaration
