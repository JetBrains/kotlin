// ERROR: Null can not be a value of a non-null type String
// ERROR: Null can not be a value of a non-null type String
import java.util.*

internal class A {
    fun foo() {
        val list = listOf<String>(null)
        val set = setOf<String>(null)
    }
}