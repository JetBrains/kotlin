package kt82160

internal interface I<T> {
    fun foo82160(): T = TODO()
}

internal interface I2<T> {
    fun bar82160(x: T) { }
}

class C : I<Int>
open class D : I<Int>

class C2 : I2<Int>
open class D2 : I2<Int>