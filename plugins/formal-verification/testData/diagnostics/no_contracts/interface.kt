interface Foo {
    var varProp: Int
    val valProp: Int
}

fun <!VIPER_TEXT!>test_properties<!>(foo: Foo) {
    foo.varProp = 0
    val x = foo.varProp + foo.valProp
}