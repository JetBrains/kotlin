import kotlinApi.*

internal class A {
    internal fun foo(t: KotlinTrait): Int {
        return t.nullableFun()!!.length() + t.notNullableFun().length()
    }
}