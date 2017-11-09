import kotlin.test.*

var value = 5

class SimpleTest {

    @BeforeTest fun beforeFun() {
        value *= 2
    }

    @AfterTest fun afterFun() {
        value /= 2
    }

    @Test fun testFoo() {
        assertNotEquals(value, foo())
    }

    @Test fun testBar() {
        assertEquals(value, foo())
    }

    @Ignore @Test fun testFooWrong() {
        assertEquals(20, foo())
    }

}

@Ignore
class TestTest {
    @Test fun emptyTest() {
    }
}