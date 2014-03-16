package foo

trait A : B, E
trait B
open class C {
    fun foo() = true
}
trait D
trait E
trait F : G, D
trait G

fun box() = C().foo()