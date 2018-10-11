package test.sample

import sample.Hello
import kotlin.test.Test

class SampleTest {
    @Test
    fun dummy(): Unit {
        Hello().doSomething()
    }
}
