import kotlinApi.*

class C : KotlinClass() {
    fun foo() {
        System.out.println(property)
        property = "a"
    }
}