// ERROR: No value passed for parameter field
import kotlinApi.*

internal class C : KotlinClass() {
    internal fun foo() {
        println(property)
        property = "a"
    }
}