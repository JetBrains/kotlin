import org.jetbrains.kotlin.fir.plugin.WithHello
import org.jetbrains.kotlin.fir.plugin.WithGenerated
import org.jetbrains.kotlin.fir.plugin.AllOpen

@WithGenerated
class A {
    @WithHello
    val x: Int = 1
}

class B {
    @WithHello
    val x: Int = 1
}

fun test_1(a: A) {
    a.helloX() // should be OK
}

fun test_2(b: B) {
    b.<!UNRESOLVED_REFERENCE!>helloX<!>() // should be an error
}