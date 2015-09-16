import kotlinApi.*

internal class C {
    fun foo(k: KotlinClass) {
        println(k.field)
        k.field = 1
    }
}