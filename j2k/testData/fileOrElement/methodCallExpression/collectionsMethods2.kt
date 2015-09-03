// ERROR: Null can not be a value of a non-null type kotlin.String
// ERROR: Null can not be a value of a non-null type kotlin.String
import java.util.*

class A {
    fun foo() {
        val list = listOf<String>(null)
        val set = setOf<String>(null)
    }
}