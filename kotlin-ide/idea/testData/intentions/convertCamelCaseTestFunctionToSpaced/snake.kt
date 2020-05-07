// CONFIGURE_LIBRARY: JUnit
// IS_APPLICABLE: false

import org.junit.Test

class A {
    @Test fun <caret>test_two_plus_two_equals_four() {}
}

fun test() {
    A().test_two_plus_two_equals_four()
}