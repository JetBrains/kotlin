// EXPECTED_REACHABLE_NODES: 542
package foo


class TestInternal {
    internal fun foo(): Int = 1
    internal fun foo(i: Int): Int = 2

    internal fun boo(): Int = 1
    internal fun boo(i: Int): Int = 2
    internal fun boo(s: String): Int = 3
}

val internal_in_class_f = { TestInternal().foo() + TestInternal().foo(1) }
val internal_in_class_b = { TestInternal().boo() + TestInternal().boo(1) }


class TestPublic {
    public fun foo(): Int = 1
    public fun foo(i: Int): Int = 2

    public fun boo(i: Int): Int = 2
    public fun boo(s: String): Int = 3
    public fun boo(): Int = 1
}

val public_in_class_f = { TestPublic().foo() + TestPublic().foo(1) }
val public_in_class_b = { TestPublic().boo() + TestPublic().boo(1) }


class TestPrivate {
    private fun foo(): Int = 1
    private fun foo(i: Int): Int = 2

    private fun boo(): Int = 1
    private fun boo(i: Int): Int = 2
    private fun boo(s: String): Int = 3

    val f = { foo() + foo(1) }
    val b = { boo() + boo(1) }
}

val private_in_class_f = TestPrivate().f
val private_in_class_b = TestPrivate().b


class TestMixed {
    public fun foo(s: String): Int = 3
    fun foo(): Int = 1
    private fun foo(s: String, i: Int): Int = 4
    fun foo(i: Int): Int = 2

    fun boo(i: Int): Int = 2
    private fun boo(s: String, i: Int): Int = 4
    public fun boo(s: String): Int = 3
    fun boo(): Int = 1

    val f = { foo() + foo(1) }
    val b = { boo() + boo(1) }
}

val mixed_in_class_f = TestMixed().f
val mixed_in_class_b = TestMixed().b

interface TestPublicInTrait {
    @JsName("foo") fun foo(): Int = 2
    @JsName("fooProp") val foo: Int
    @JsName("booProp") val boo: Int
    @JsName("boo") fun boo(): Int = 2
}

val public_in_trait_f = { obj: TestPublicInTrait -> obj.foo() + obj.foo }
val public_in_trait_b = { obj: TestPublicInTrait -> obj.boo() + obj.boo }

//Testing

private val functionRegex = RegExp("function\\s+[a-z0-9\$_]+\\(")

fun test(testName: String, ff: Any, fb: Any) {
    val f = ff.toString().replace(functionRegex, "")
    val b = fb.toString().replaceAll("boo", "foo").replace(functionRegex, "")

    if (f != b) fail("FAILED on ${testName}:\n f = \"$f\"\n b = \"$b\"")
}

fun box(): String {
    test("internal_in_class", internal_in_class_f, internal_in_class_b)
    test("public_in_class", public_in_class_f, public_in_class_b)
    test("private_in_class", private_in_class_f, private_in_class_b)
    test("mixed_in_class", mixed_in_class_f, mixed_in_class_b)

    test("public_in_trait", public_in_trait_f, public_in_trait_b)

    return "OK"
}
