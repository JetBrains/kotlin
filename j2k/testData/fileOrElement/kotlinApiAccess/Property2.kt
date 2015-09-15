import kotlinApi.*

internal class C {
    internal fun foo(k: KotlinClass) {
        println(k.field)
        k.field = 1
    }
}