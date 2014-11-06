// ERROR: No value passed for parameter field
import kotlinApi.*

class C : KotlinClass() {
    fun foo() {
        System.out.println(property)
        property = "a"
    }
}