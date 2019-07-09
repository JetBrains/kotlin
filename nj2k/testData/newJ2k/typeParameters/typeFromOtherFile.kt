import javaApi.T
import java.util.Collections

internal class A {
    fun foo(t: T): Any {
        return Collections.nCopies(1, t.set)
    }
}