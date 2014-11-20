import java.util.*
import javaApi.T

class A {
    public fun foo(t: T): Any {
        return Collections.nCopies<Set<String>>(1, t.set)
    }
}