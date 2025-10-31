import kotlin.test.Test
import kotlin.test.assertTrue

class TestClient {
    @Test
    fun testGreet() {
        assertTrue { true }
        // KT-82073: requires exposing require into import-objects file
        // assertFalse("No require found", ::checkRequire)
    }
}

// KT-82073: requires exposing require into import-objects file
//@JsFun("() => require === undefined")
//external fun checkRequire(): Boolean