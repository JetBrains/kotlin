// MINIFICATION_THRESHOLD: 1519
package foo

private var log = ""

public fun public_baz(i: Int) {
    log = "public_baz"
}
external public fun public_baz(a: String) {
    definedExternally
}

internal fun internal_baz(i: Int) {
}
internal external fun internal_baz(a: String) {
    definedExternally
}

private fun getCurrentPackage(): dynamic = eval("_").foo

private fun private_baz(i: Int) {
}
private external fun private_baz(a: String) {
    definedExternally
}

public class PublicClass {
    public fun public_baz(i: Int) {
    }
    @JsName("public_baz")
    public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }

    @JsName("internal_baz")
    internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }

    @JsName("private_baz")
    private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

internal class InternalClass {
    public fun public_baz(i: Int) {
    }

    @JsName("public_baz")
    public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }

    @JsName("internal_baz")
    internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }

    @JsName("private_baz")
    private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

private class PrivateClass {
    public fun public_baz(i: Int) {
    }

    @JsName("public_baz")
    public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @JsName("internal_baz")
    internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @JsName("private_baz")
    private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

open public class OpenPublicClass {
    public fun public_baz(i: Int) {
    }
    @JsName("public_baz")
    public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @JsName("internal_baz")
    internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @JsName("private_baz")
    private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

internal open class OpenInternalClass {
    public fun public_baz(i: Int) {
    }
    @JsName("public_baz")
    public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @JsName("internal_baz")
    internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @JsName("private_baz")
    private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

open private class OpenPrivateClass {
    public fun public_baz(i: Int) {
    }
    @JsName("public_baz")
    public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @JsName("internal_baz")
    internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @JsName("private_baz")
    private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

// Helpers

val CALEE_NAME = RegExp("""\b\w*(baz[^(]*)""")

fun Function0<Unit>.extractNames(): Array<String> {
    val names = CALEE_NAME.exec(this.toString())

    if (names == null || names.size != 2) {
        throw Exception("Cannot extract function name, $names for actual = \"$this\"")
    }

    return names
}

// Testing

var testGroup = ""

fun test(expected: String, f: () -> Unit) {
    val actual = f.extractNames()

    if (expected != actual[1]) {
        fail("Failed on '$testGroup' group: expected = \"$expected\", actual[1] = \"${actual[1]}\"\n actual = $actual")
    }
}

val privateMangledRegex = Regex("baz_[0-9a-zA-Z]+\\\$_0")

fun testMangledPrivate(f: () -> Unit) {
    val actual = f.extractNames()

    if (!privateMangledRegex.matches(actual[1])) {
        fail("Failed on '$testGroup' group: actual[1] = \"${actual[1]}\"\n actual = $actual, should look like 'baz_<hash>_0'")
    }
    if ("_za3lpa\$" in actual[1]) {
        fail("Failed on '$testGroup' group: actual[1] = \"${actual[1]}\"\n actual = $actual, should not contains 'za3lpa\$'")
    }
}

class Dummy {
    public fun stable_mangled_baz(i: Int) { }
}

val SIMPLE = "baz"
val SIMPLE0 = "${SIMPLE}_0"
val NATIVE = SIMPLE
val STABLE = "baz_za3lpa$"
val INTERNAL = "baz_kcn2v3$"

fun box(): String {
    testGroup = "Top Level"

    getCurrentPackage().`public_baz_za3lpa$`(0)
    assertEquals("public_baz", log)
    test(NATIVE) { public_baz("native") }

    testGroup = "Public Class"
    test(STABLE) { PublicClass().public_baz(0) }
    test(NATIVE) { PublicClass().public_baz("native") }
    test(INTERNAL) { PublicClass().internal_baz(0) }
    test(NATIVE) { PublicClass().internal_baz("native") }
    test(SIMPLE0, PublicClass().call_private_baz)
    test(NATIVE, PublicClass().call_private_native_baz)

    testGroup = "Internal Class"
    test(STABLE) { InternalClass().public_baz(0) }
    test(NATIVE) { InternalClass().public_baz("native") }
    test(INTERNAL) { InternalClass().internal_baz(0) }
    test(NATIVE) { InternalClass().internal_baz("native") }
    test(SIMPLE0, InternalClass().call_private_baz)
    test(NATIVE, InternalClass().call_private_native_baz)

    testGroup = "Private Class"
    test(STABLE) { PrivateClass().public_baz(0) }
    test(NATIVE) { PrivateClass().public_baz("native") }
    test(INTERNAL) { PrivateClass().internal_baz(0) }
    test(NATIVE) { PrivateClass().internal_baz("native") }
    test(SIMPLE0, PrivateClass().call_private_baz)
    test(NATIVE, PrivateClass().call_private_native_baz)

    testGroup = "Open Public Class"
    test(STABLE) { OpenPublicClass().public_baz(0) }
    test(NATIVE) { OpenPublicClass().public_baz("native") }
    test(INTERNAL) { OpenPublicClass().internal_baz(0) }
    test(NATIVE) { OpenPublicClass().internal_baz("native") }
    testMangledPrivate(OpenPublicClass().call_private_baz)
    test(NATIVE, OpenPublicClass().call_private_native_baz)

    testGroup = "Open Internal Class"
    test(STABLE) { OpenInternalClass().public_baz(0) }
    test(NATIVE) { OpenInternalClass().public_baz("native") }
    test(INTERNAL) { OpenInternalClass().internal_baz(0) }
    test(NATIVE) { OpenInternalClass().internal_baz("native") }
    test(SIMPLE0, OpenInternalClass().call_private_baz)
    test(NATIVE, OpenInternalClass().call_private_native_baz)

    testGroup = "Open Private Class"
    test(STABLE) { OpenPrivateClass().public_baz(0) }
    test(NATIVE) { OpenPrivateClass().public_baz("native") }
    test(INTERNAL) { OpenPrivateClass().internal_baz(0) }
    test(NATIVE) { OpenPrivateClass().internal_baz("native") }
    test(SIMPLE0, OpenPrivateClass().call_private_baz)
    test(NATIVE, OpenPrivateClass().call_private_native_baz)

    return "OK"
}
