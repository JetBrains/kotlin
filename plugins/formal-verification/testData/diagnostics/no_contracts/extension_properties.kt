val Int.succ: Int get() = this + 1

var Int.strange: Int
    get() = this
    set(v) {
        val x = (v + this)
    }

fun <!VIPER_TEXT!>extensionGetterProperty<!>() {
    val a = 3.succ
    val b = 40.succ.succ
}

fun <!VIPER_TEXT!>extensionSetterProperty<!>() {
    42.strange = 0
}

class Foo(val x: Int)

val Foo.succ: Int get() = this.x + 1

var Foo.strange: Int
    get() = this.x
    set(v) {
        val x1 = (v + this.x)
    }

fun <!VIPER_TEXT!>extensionGetterPropertyUserDefinedClass<!>() {
    val f = Foo(42)
    val x = f.succ
}

fun <!VIPER_TEXT!>extensionSetterPropertyUserDefinedClass<!>() {
    val f = Foo(42)
    f.strange = 42
}