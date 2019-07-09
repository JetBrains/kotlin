import kotlinApi.KotlinClass

internal class C {
    fun foo(k: KotlinClass) {
        println(k.property)
        k.property = "a"
    }
}