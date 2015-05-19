// ERROR: None of the following functions can be called with the arguments supplied:  public fun <T> listOf(): kotlin.List<kotlin.String> defined in kotlin public fun <T> listOf(vararg values: kotlin.String): kotlin.List<kotlin.String> defined in kotlin public fun <T> listOf(value: kotlin.String): kotlin.List<kotlin.String> defined in kotlin
// ERROR: None of the following functions can be called with the arguments supplied:  public fun <T> setOf(): kotlin.Set<kotlin.String> defined in kotlin public fun <T> setOf(vararg values: kotlin.String): kotlin.Set<kotlin.String> defined in kotlin public fun <T> setOf(value: kotlin.String): kotlin.Set<kotlin.String> defined in kotlin
import java.util.*

class A {
    fun foo() {
        val list = listOf<String>(null)
        val set = setOf<String>(null)
    }
}