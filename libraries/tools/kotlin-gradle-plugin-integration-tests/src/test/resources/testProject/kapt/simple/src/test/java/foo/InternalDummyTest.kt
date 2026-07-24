package foo

import org.junit.Test

class InternalDummyTest {
    @Test
    fun testDummy() {
        val dummy = InternalDummy()
        val dummyUser = InternalDummyUser()
        dummyUser.use(dummy)
    }
}