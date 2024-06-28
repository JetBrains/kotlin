import kotlin.test.Test
import kotlin.test.assertFalse

class TestClient {
    @Test
    fun testGreet() {
        assertFalse("No require found", ::checkRequire)
    }
}

@JsFun("() => require === undefined")
external fun checkRequire(): Boolean