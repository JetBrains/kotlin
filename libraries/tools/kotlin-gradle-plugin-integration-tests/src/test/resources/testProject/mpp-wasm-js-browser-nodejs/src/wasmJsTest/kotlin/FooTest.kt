import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.Ignore

class TestClient {
    @Test
    @Ignore // KT-82073: requires exposing require into import-objects file
    fun testGreet() {
        assertFalse("No require found", ::checkRequire)
    }
}

@JsFun("() => require === undefined")
external fun checkRequire(): Boolean