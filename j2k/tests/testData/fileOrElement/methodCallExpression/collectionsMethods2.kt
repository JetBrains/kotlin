// ERROR: Too many arguments for public fun <T> listOf(): kotlin.List<T> defined in kotlin
// ERROR: Null can not be a value of a non-null type kotlin.String
import java.util.*

class A {
    fun foo() {
        val list = listOf<String>(null)
        val set = setOf<String>(null)
    }
}