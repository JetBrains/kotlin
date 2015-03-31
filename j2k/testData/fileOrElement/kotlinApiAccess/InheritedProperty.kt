// ERROR: No value passed for parameter field
import kotlinApi.*

class C : KotlinClass() {
    fun foo() {
        println(property)
        property = "a"
    }
}