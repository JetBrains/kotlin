import java.util.*
import javaApi.T

internal class A {
    fun foo(t: T): Any {
        return Collections.nCopies(1, t.set)
    }
}