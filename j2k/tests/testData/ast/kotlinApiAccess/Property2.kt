import kotlinApi.*

class C {
    fun foo(k: KotlinClass) {
        System.out.println(k.field)
        k.field = 1
    }
}