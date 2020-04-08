interface Interface<T>
interface Interface2<T>
class A<T> : Interface2<T>

abstract class Abstract<T>
abstract class Abstract2<T>
class B<T> : Abstract2<T>()

sealed class Sealed<T>
sealed class Sealed2<T>
class C<T> : Sealed2<T>()

open class Open<T>
open class Open2<T>
class D<T> : Open2<T>()

fun main(args: Array<String>) {
    Interface::class
    Interface2::class
    A::class

    Abstract::class
    Abstract2::class
    B::class

    Sealed::class
    Sealed2::class
    C::class

    Open::class
    Open2::class
    D::class
}