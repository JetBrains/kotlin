// CONFIGURE_LIBRARY: JUnit@lib/junit-4.12.jar
import org.junit.Test

class A {
    @Test fun <caret>testTwoPlusTwoEqualsFour() {}
}

fun test() {
    A().testTwoPlusTwoEqualsFour()
}