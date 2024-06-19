// NEVER_VALIDATE

interface A {
    val field: Int
        get() = 0
}

interface B: A

abstract class C: A {
    override val field = 0
}

class D: B, C()

fun <!VIPER_TEXT!>testDiamond<!>() = D().field

interface E {
    val field: Any
}

abstract class F {
    open var field: Int = 0
}

abstract class H {
    var field: Int = 0
        set(value) {
            Unit
        }
}

class G: E, F()
class I: E, H()

fun <!VIPER_TEXT!>testVarVal<!>() {
    val g = G()
    g.field
    g.field = 1

    val i = I()
    i.field
    i.field = 1
}
