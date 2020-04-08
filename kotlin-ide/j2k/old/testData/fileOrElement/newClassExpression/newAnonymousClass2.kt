internal abstract class A

internal class C {
    fun foo() {
        val a = object : A() {
            override fun toString(): String {
                return "a"
            }
        }
    }
}