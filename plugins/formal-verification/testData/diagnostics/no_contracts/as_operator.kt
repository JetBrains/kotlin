open class Foo
class Bar : Foo()

fun <!VIPER_TEXT!>testAs<!>(foo: Foo): Bar = foo as Bar

fun <!VIPER_TEXT!>testNullableAs<!>(foo: Foo?): Bar? = foo as Bar?

fun <!VIPER_TEXT!>testSafeAs<!>(foo: Foo): Bar? = foo as? Bar

fun <!VIPER_TEXT!>testNullableSafeAs<!>(foo: Foo?): Bar? = foo as? Bar
