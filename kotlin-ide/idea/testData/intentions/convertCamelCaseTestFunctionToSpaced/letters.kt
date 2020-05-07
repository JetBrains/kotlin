// CONFIGURE_LIBRARY: JUnit
import org.junit.Test

class A {
    @Test fun <caret>testTwoPlusTwoEqualsFour() {}
}

fun test() {
    A().testTwoPlusTwoEqualsFour()
}