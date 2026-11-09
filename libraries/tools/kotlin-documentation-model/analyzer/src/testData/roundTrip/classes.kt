// MODULE: main
// FILE: example/Classes.kt
package example

abstract class AbstractApi<T : CharSequence> {
    companion object {
        external fun create(): AbstractApi<String>

        class Options(val enabled: Boolean = true)
    }

    context(label: String, _: Int)
    abstract fun convert(value: T, prefix: String = "", suffix: String = value.toString()): String

    context(key: String)
    abstract val items: List<T?>

    class Nested

    abstract var selected: T?
}

abstract class AbstractEmpty

class Empty

class Generic<T> {
    companion object Factory {
        external fun create(): Generic<String>
    }
}

class Parameters(name: String = "default", count: Int = name.length)

class Properties(val name: String = "sample", var count: Int = 42)

object Registry {
    class Entry(val name: String = "default")

    external fun lookup(name: String = "default"): Entry
}

object Singleton
