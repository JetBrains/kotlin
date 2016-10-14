package foo

public fun public_baz(i: Int) {
}
@native public fun public_baz(a: String) {
}

internal fun internal_baz(i: Int) {
}
internal @native fun internal_baz(a: String) {
}

private fun private_baz(i: Int) {
}
@native private fun private_baz(a: String) {
}

public class PublicClass {
    public fun public_baz(i: Int) {
    }
    @native public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @native internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @native private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

internal class InternalClass {
    public fun public_baz(i: Int) {
    }
    @native public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @native internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @native private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

private class PrivateClass {
    public fun public_baz(i: Int) {
    }
    @native public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @native internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @native private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

open public class OpenPublicClass {
    public fun public_baz(i: Int) {
    }
    @native public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @native internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @native private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

internal open class OpenInternalClass {
    public fun public_baz(i: Int) {
    }
    @native public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @native internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @native private fun private_baz(a: String) {
    }

    val call_private_baz = { private_baz(0) }
    val call_private_native_baz = { private_baz("native") }
}

open private class OpenPrivateClass {
    public fun public_baz(i: Int) {
    }
    @native public fun public_baz(a: String) {
    }

    internal fun internal_baz(i: Int) {
    }
    @native internal fun internal_baz(a: String) {
    }

    private fun private_baz(i: Int) {
    }
    @native private fun private_baz(a: String) {
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

public fun stable_mangled_baz(i: Int) {
}

val SIMPLE = "baz"
val SIMPLE0 = "${SIMPLE}_0"
val NATIVE = SIMPLE
val STABLE = { stable_mangled_baz(0) }.extractNames()[1]

fun box(): String {
    testGroup = "Top Level"
    test(STABLE) { public_baz(0) }
    test(NATIVE) { public_baz("native") }
    test(SIMPLE0) { internal_baz(0) }
    test(NATIVE) { internal_baz("native") }
    test(SIMPLE0) { private_baz(0) }
    test(NATIVE) { private_baz("native") }

    testGroup = "Public Class"
    test(STABLE) { PublicClass().public_baz(0) }
    test(NATIVE) { PublicClass().public_baz("native") }
    test(SIMPLE0) { PublicClass().internal_baz(0) }
    test(NATIVE) { PublicClass().internal_baz("native") }
    test(SIMPLE0, PublicClass().call_private_baz)
    test(NATIVE, PublicClass().call_private_native_baz)

    testGroup = "Internal Class"
    test(STABLE) { InternalClass().public_baz(0) }
    test(NATIVE) { InternalClass().public_baz("native") }
    test(SIMPLE0) { InternalClass().internal_baz(0) }
    test(NATIVE) { InternalClass().internal_baz("native") }
    test(SIMPLE0, InternalClass().call_private_baz)
    test(NATIVE, InternalClass().call_private_native_baz)

    testGroup = "Private Class"
    test(STABLE) { PrivateClass().public_baz(0) }
    test(NATIVE) { PrivateClass().public_baz("native") }
    test(SIMPLE0) { PrivateClass().internal_baz(0) }
    test(NATIVE) { PrivateClass().internal_baz("native") }
    test(SIMPLE0, PrivateClass().call_private_baz)
    test(NATIVE, PrivateClass().call_private_native_baz)

    testGroup = "Open Public Class"
    test(STABLE) { OpenPublicClass().public_baz(0) }
    test(NATIVE) { OpenPublicClass().public_baz("native") }
    testMangledPrivate { OpenPublicClass().internal_baz(0) }
    test(NATIVE) { OpenPublicClass().internal_baz("native") }
    testMangledPrivate(OpenPublicClass().call_private_baz)
    test(NATIVE, OpenPublicClass().call_private_native_baz)

    testGroup = "Open Internal Class"
    test(STABLE) { OpenInternalClass().public_baz(0) }
    test(NATIVE) { OpenInternalClass().public_baz("native") }
    test(SIMPLE0) { OpenInternalClass().internal_baz(0) }
    test(NATIVE) { OpenInternalClass().internal_baz("native") }
    test(SIMPLE0, OpenInternalClass().call_private_baz)
    test(NATIVE, OpenInternalClass().call_private_native_baz)

    testGroup = "Open Private Class"
    test(STABLE) { OpenPrivateClass().public_baz(0) }
    test(NATIVE) { OpenPrivateClass().public_baz("native") }
    test(SIMPLE0) { OpenPrivateClass().internal_baz(0) }
    test(NATIVE) { OpenPrivateClass().internal_baz("native") }
    test(SIMPLE0, OpenPrivateClass().call_private_baz)
    test(NATIVE, OpenPrivateClass().call_private_native_baz)

    return "OK"
}
