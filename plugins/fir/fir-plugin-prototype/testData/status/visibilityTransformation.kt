import org.jetbrains.kotlin.fir.plugin.AllPublic
import org.jetbrains.kotlin.fir.plugin.Visibility

@AllPublic(Visibility.Protected)
class A {
    val x: String = ""

    fun foo() {}

    class Nested {
        fun bar() {

        }
    }
}

@AllPublic(Visibility.Private)
class B {
    val x: String = ""

    fun foo() {

    }

    class Nested {
        fun bar() {

        }
    }
}
