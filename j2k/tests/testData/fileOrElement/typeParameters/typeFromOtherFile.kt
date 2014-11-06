import java.util.*
import javaApi.T
import kotlin.Set

class A {
    public fun foo(t: T): Any {
        return Collections.nCopies<Set<String>>(1, t.set)
    }
}