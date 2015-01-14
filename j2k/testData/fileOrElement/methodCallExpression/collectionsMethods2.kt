// ERROR: Too many arguments for public fun <T> listOf(): kotlin.List<T> defined in kotlin
// ERROR: Too many arguments for public fun <T> setOf(): kotlin.Set<T> defined in kotlin
import java.util.*

class A {
    fun foo() {
        val list = listOf<String>(null)
        val set = setOf<String>(null)
    }
}