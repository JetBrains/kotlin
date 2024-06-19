// NEVER_VALIDATE

open class FieldA
open class FieldB : FieldA()
class C

open class A(val fieldNotOverride: C) {
    open val fieldOverride: FieldA = FieldA()
}

open class B(override val fieldOverride: FieldB) : A(C())

fun <!VIPER_TEXT!>createB<!>() {
    val fieldB = FieldB()
    val b = B(fieldB)
    val fieldOverride = b.fieldOverride //should verify that fieldOverride == fieldB
    val fieldNotOverride = b.fieldNotOverride
}

open class FirstBackingFieldClass {
    open val x: Int = 1
}

open class NoBackingFieldClass: FirstBackingFieldClass() {
    override val x: Int
        get() = 1
}

class SecondBackingFieldClass(override val x: Int) : NoBackingFieldClass()

// BF stands for backing field
fun <!VIPER_TEXT!>createBFsAndNoBF<!>() {
    val fbf = FirstBackingFieldClass()
    val fbfx = fbf.x
    val nbf = NoBackingFieldClass()
    val nbfx = nbf.x
    val sbf = SecondBackingFieldClass(10)
    val sbfx = sbf.x
}

// checks that we don't consider value parameter `a` of `Y`'s constructor a field
open class X(val a: Int)
class Y(a: Int) : X(0)

fun <!VIPER_TEXT!>createY<!>() {
    val y = Y(10)
    val ya = y.a
}
