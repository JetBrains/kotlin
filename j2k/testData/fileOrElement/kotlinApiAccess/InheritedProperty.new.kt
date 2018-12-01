// ERROR: No value passed for parameter 'field'
import kotlinApi.property

internal class C : KotlinClass() {
    fun foo() {
        println(property)
        property = "a"
    }
}