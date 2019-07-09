// ERROR: No value passed for parameter 'field'
import kotlinApi.KotlinClass

internal class C : KotlinClass() {
    fun foo() {
        println(property)
        property = "a"
    }
}