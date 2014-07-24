import kotlinApi.*

class C {
    fun foo(k: KotlinClass) {
        System.out.println(k.property)
        k.property = "a"
    }
}