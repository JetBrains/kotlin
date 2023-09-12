import org.jetbrains.kotlin.formver.plugin.NeverConvert
class Bar(var a: Int)

class Foo(var b: Bar)

class Person(var age: Int)

class Baz {
    private var _a: Int = 0
    var a: Int
        get() = _a
        set(value) {
            _a = value
        }
}

class AlwaysPlusOne {
    var b = 0
    var num: Int = 0
        get() = field
        set(value) {
            field = value + 1
        }
}

fun <!VIPER_TEXT!>testCustomSetter<!>() {
    val f = AlwaysPlusOne()
    f.b = 1
    f.num = 1
}

fun <!VIPER_TEXT!>testSetter<!>() {
    val person = Person(19)
    person.age = person.age + 1
}

fun <!VIPER_TEXT!>testSetterCascade<!>() {
    val f = Foo(Bar(10))
    f.b.a = 42
}

fun <!VIPER_TEXT!>testSetterNoBackingField<!>() {
    val baz = Baz()
    baz.a = 42
}

@NeverConvert
@Suppress("NOTHING_TO_INLINE")
inline fun mockInline(b: Bar) {
    b.a = 42
}

fun <!VIPER_TEXT!>testSetterLambda<!>() {
    mockInline(Bar(0))
}