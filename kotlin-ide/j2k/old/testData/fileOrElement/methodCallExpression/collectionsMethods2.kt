// ERROR: Null can not be a value of a non-null type TypeVariable(T)
// ERROR: Null can not be a value of a non-null type TypeVariable(T)
import java.util.*

internal class A {
    fun foo() {
        val list = listOf<String>(null)
        val set = setOf<String>(null)
    }
}