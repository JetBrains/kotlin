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

fun <T> Property<T>.assign(value: T?) = set(value)
fun <T> Property<T>.assign(provider: Provider<out T?>) = set(provider)

class PropertyImpl<T>(private var value: T? = null) : Property<T> {
    private var provider: Provider<out T?>? = null

    override fun get(): T = provider?.get() ?: value!!

    override fun set(t: T?) {
        value = t
        provider = null
    }

    override fun set(provider: Provider<out T?>) {
        this.provider = provider
    }
}

enum class Foo {
    BAR, BAZ
}

class PropertyRepro {
    val foo: Property<Foo> = PropertyImpl()
}

fun box(): String {
    val repro = PropertyRepro()

    repro.foo = BAR
    if (repro.foo.get() != Foo.BAR) return "Fail 1: ${repro.foo.get()}"

    repro.foo = BAZ
    if (repro.foo.get() != Foo.BAZ) return "Fail 2: ${repro.foo.get()}"

    repro.foo.set(BAR)
    if (repro.foo.get() != Foo.BAR) return "Fail 3: ${repro.foo.get()}"

    repro.foo = PropertyImpl(Foo.BAZ)
    if (repro.foo.get() != Foo.BAZ) return "Fail 4: ${repro.foo.get()}"

    repro.apply {
        foo = BAR
    }
    if (repro.foo.get() != Foo.BAR) return "Fail 5: ${repro.foo.get()}"

    return "OK"
}
