package test.sample

import sample.Hello

import org.junit.Test

class SampleTest {
    @Test
    fun dummy(): Unit {
        Hello().doSomething()
    }
}
