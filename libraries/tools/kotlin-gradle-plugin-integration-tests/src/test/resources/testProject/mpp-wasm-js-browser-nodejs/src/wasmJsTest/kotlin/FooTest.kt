import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFails
import kotlin.test.assertTrue

private const val DO_NOT_USE_REQUIRE = "Do not use top-level require"
private const val TOP_LEVEL_REQUIRE_ERROR_URL = "https://kotl.in/r9txlt"

class TestClient {
    @Test
    fun testGreet() {
        assertFalse("No require found", ::checkRequire)
        assertEquals("function", requireType())

        assertFails("No require cache found", ::checkRequireCache)
        assertTopLevelRequireErrorMessage(checkRequireCacheErrorMessage())

        assertFails("No require resolve found", ::checkRequireResolve)
        assertTopLevelRequireErrorMessage(checkRequireResolveErrorMessage())

        assertFails(::fs)
        assertTopLevelRequireErrorMessage(fsErrorMessage())
        assertTrue(topLevelRequireThrowsErrorInstance())
        assertTopLevelRequireErrorMessage(fsWithNullRequireErrorMessage())

        assertFails(::loadDependencyViaRequire)
        assertTopLevelRequireErrorMessage(loadDependencyViaRequireErrorMessage())

        assertEquals("fs:fs-resolved:cache-ok", fsWithCustomRequire())
        assertFails(::fs)

        withCustomRequire {
            assertEquals("dependency", loadDependencyViaRequire())
        }
        assertFails(::loadDependencyViaRequire)

        assertEquals("dependency", localRequireShadowing())
    }

    private fun withCustomRequire(block: () -> Unit) {
        defineCustomRequire()
        try {
            block()
        } finally {
            restoreCustomRequire()
        }
    }

    private fun assertTopLevelRequireErrorMessage(message: String) {
        assertContains(message, DO_NOT_USE_REQUIRE)
        assertContains(message, TOP_LEVEL_REQUIRE_ERROR_URL)
    }
}

@JsFun("() => require === undefined")
external fun checkRequire(): Boolean

@JsFun("() => typeof require")
external fun requireType(): String

@JsFun("() => require.cache")
external fun checkRequireCache(): Boolean

@JsFun("""
    () => {
        try {
            require.cache
            return "OK"
        } catch (e) {
            return String(e.message)
        }
    }
""")
external fun checkRequireCacheErrorMessage(): String

@JsFun("() => require.resolve")
external fun checkRequireResolve(): JsAny

@JsFun("""
    () => {
        try {
            require.resolve
            return "OK"
        } catch (e) {
            return String(e.message)
        }
    }
""")
external fun checkRequireResolveErrorMessage(): String

@JsFun("() => require(\"fs\")")
external fun fs(): JsAny

@JsFun("""
    () => {
        try {
            require("fs")
            return "OK"
        } catch (e) {
            return String(e.message)
        }
    }
""")
external fun fsErrorMessage(): String

@JsFun("""
    () => {
        try {
            require("fs")
            return false
        } catch (e) {
            return e instanceof Error
        }
    }
""")
external fun topLevelRequireThrowsErrorInstance(): Boolean

@JsFun("""
    () => {
        const previousRequire = globalThis.require
        try {
            globalThis.require = null
            require("fs")
            return "OK"
        } catch (e) {
            return String(e.message)
        } finally {
            if (previousRequire === undefined) {
                delete globalThis.require
            } else {
                globalThis.require = previousRequire
            }
        }
    }
""")
external fun fsWithNullRequireErrorMessage(): String

@JsFun("""
    () => {
        const previousRequire = globalThis.require
        try {
            const customRequire = (name) => ({ requested: name, id: name })
            customRequire.cache = "cache-ok"
            customRequire.resolve = (name) => name + "-resolved"
            globalThis.require = customRequire
            return require("fs").requested + ":" + require.resolve("fs") + ":" + require.cache
        } finally {
            if (previousRequire === undefined) {
                delete globalThis.require
            } else {
                globalThis.require = previousRequire
            }
        }
    }
""")
external fun fsWithCustomRequire(): String

@JsFun("""
    () => {
        const require = (name) => ({ id: name })
        return require("dependency").id
    }
""")
external fun localRequireShadowing(): String

@JsFun("""
    () => {
        globalThis.__kotlinTestPreviousRequire = globalThis.require
        const customRequire = (name) => ({ requested: name, id: name })
        customRequire.cache = "cache-ok"
        customRequire.resolve = (name) => name + "-resolved"
        globalThis.require = customRequire
    }
""")
external fun defineCustomRequire()

@JsFun("""
    () => {
        if (globalThis.__kotlinTestPreviousRequire === undefined) {
            delete globalThis.require
        } else {
            globalThis.require = globalThis.__kotlinTestPreviousRequire
        }
        delete globalThis.__kotlinTestPreviousRequire
    }
""")
external fun restoreCustomRequire()
