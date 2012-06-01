package testPackage

import org.junit.Test as test

class SimpleTest {

    public fun testFoo() {
        val name = "world"
        val message = "hello $name!"
    }

    test fun cheese() {
        val name = "world"
        val message = "bye $name!"
    }
}