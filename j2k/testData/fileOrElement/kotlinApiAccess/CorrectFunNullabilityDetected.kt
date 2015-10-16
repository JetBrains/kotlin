import kotlinApi.*

internal class A {
    fun foo(t: KotlinTrait): Int {
        return t.nullableFun()!!.length + t.notNullableFun().length
    }
}