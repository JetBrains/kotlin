import kotlinApi.*

internal class C {
    internal fun foo(k: KotlinClass) {
        println(k.property)
        k.property = "a"
    }
}