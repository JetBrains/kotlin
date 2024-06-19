// NEVER_VALIDATE

interface Foo {
    var varProp: Int
    val valProp: Int
}

fun <!VIPER_TEXT!>testProperties<!>(foo: Foo) {
    foo.varProp = 0
    val x = foo.varProp + foo.valProp
}

interface First {
    val number: Int
        get() = 1
}

interface Second {
    val number: Int
        get() = 2
}

class Impl(override val number: Int): First, Second

fun <!VIPER_TEXT!>createImpl<!>() {
    val impl = Impl(-1)
    val implField = impl.number
}
