import org.jetbrains.kotlin.fir.plugin.CompanionWithFoo

fun context() {
    @CompanionWithFoo
    class SomeClass {

        @CompanionWithFoo
        <!NESTED_CLASS_NOT_ALLOWED!>class Nested<!>
    }

    fun takeInt(x: Int) {}

    fun test() {
        takeInt(SomeClass.foo())
        takeInt(SomeClass.Companion.foo())

        takeInt(SomeClass.Nested.foo())
        takeInt(SomeClass.Nested.Companion.foo())
    }
}
