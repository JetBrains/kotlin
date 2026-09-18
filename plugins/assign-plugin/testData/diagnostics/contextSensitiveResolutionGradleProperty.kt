// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// ISSUE: KT-81558

annotation class ValueContainer

interface Provider<T> {
    fun get(): T
}

@ValueContainer
interface Property<T> : Provider<T> {
    fun set(t: T?)
    fun set(provider: Provider<out T?>)
}

fun <T> Property<T>.assign(value: T?) {}
fun <T> Property<T>.assign(provider: Provider<out T?>) {}

abstract class PropertyRepro {
    abstract var fooPlainProperty: Foo
    abstract val foo: Property<Foo>
}

enum class Foo {
    BAR, BAZ
}

fun propertyRepro(propertyRepro: PropertyRepro, provider: Provider<Foo>) {
    propertyRepro.fooPlainProperty = BAR

    propertyRepro.foo = Foo.BAR
    propertyRepro.foo = provider

    propertyRepro.foo.set(BAR)
    propertyRepro.foo = BAR
    propertyRepro.foo <!NO_APPLICABLE_ASSIGN_METHOD!>=<!> <!UNRESOLVED_REFERENCE!>UNRESOLVED<!>

    propertyRepro.apply {
        foo = BAZ
    }
}
