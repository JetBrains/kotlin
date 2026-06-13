import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertFails
import kotlin.test.Ignore

class TestClient {
    @Test
    fun testGreet() {
        assertFalse("No require found", ::checkRequire)

        assertFails("No require cache found", ::checkRequireCache)

        assertFails(::fs)
    }
}

@JsFun("() => require === undefined")
external fun checkRequire(): Boolean

@JsFun("() => require.cache")
external fun checkRequireCache(): Boolean

@JsFun("() => require(\"fs\")")
external fun fs(): JsAny
