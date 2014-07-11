package foo

fun internal_foo(): Int = 1
native fun internal_foo(a: Array<Int>) = "should be ingnored"
fun internal_foo(i: Int): Int = 2

fun internal_boo(i: Int): Int = 2
fun internal_boo(s: String): Int = 3
fun internal_boo(): Int = 1
native fun internal_boo(a: Array<Int>) = "should be ingnored"

val internal_f = { internal_foo() + internal_foo(1) }
val internal_b = { internal_boo() + internal_boo(1) }


public fun public_foo(): Int = 1
native public fun public_foo(a: Array<Int>): String = "should be ingnored"
public fun public_foo(i: Int): Int = 2

public fun public_boo(i: Int): Int = 2
public fun public_boo(s: String): Int = 3
public fun public_boo(): Int = 1
native public fun public_boo(a: Array<Int>): String = "should be ingnored"

val public_f = { public_foo() + public_foo(1) }
val public_b = { public_boo() + public_boo(1) }


native private fun private_foo(a: Array<Int>): String = "should be ingnored"
private fun private_foo(): Int = 1
private fun private_foo(i: Int): Int = 2

native private fun private_boo(a: Array<Int>): String = "should be ingnored"
private fun private_boo(i: Int): Int = 2
private fun private_boo(s: String): Int = 3
private fun private_boo(): Int = 1

val private_f = { private_foo() + private_foo(1) }
val private_b = { private_boo() + private_boo(1) }


public fun mixed_foo(s: String): Int = 3
fun mixed_foo(): Int = 1
native fun mixed_foo(a: Array<Int>) = "should be ingnored"
private fun mixed_foo(s: String, i: Int): Int = 4
fun mixed_foo(i: Int): Int = 2

fun mixed_boo(i: Int): Int = 2
private fun mixed_boo(s: String, i: Int): Int = 4
native fun mixed_boo(a: Array<Int>) = "should be ingnored"
public fun mixed_boo(s: String): Int = 3
fun mixed_boo(): Int = 1

val mixed_f = { mixed_foo() + mixed_foo(1) + mixed_foo("str") + mixed_foo("str", 44) }
val mixed_b = { mixed_boo() + mixed_boo(1) + mixed_boo("str") + mixed_boo("str", 44) }


class TestInternal {
    fun foo(): Int = 1
    fun foo(i: Int): Int = 2

    fun boo(i: Int): Int = 2
    fun boo(s: String): Int = 3
    fun boo(): Int = 1
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

    private fun boo(i: Int): Int = 2
    private fun boo(s: String): Int = 3
    private fun boo(): Int = 1

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

//Testing

fun test(testName: String, ff: Any, fb: Any) {
    val f = ff.toString()
    val b = fb.toString().replaceAll("boo", "foo")

    if (f != b) throw Exception("FAILED on ${testName}:\n f = \"$f\"\n b = \"$b\"")
}

fun box(): String {
    test("internal", internal_f, internal_b)
    test("public", public_f, public_b)
    test("private", private_f, private_b)
    test("mixed", mixed_f, mixed_b)

    test("internal_in_class", internal_in_class_f, internal_in_class_b)
    test("public_in_class", public_in_class_f, public_in_class_b)
    test("private_in_class", private_in_class_f, private_in_class_b)
    test("mixed_in_class", mixed_in_class_f, mixed_in_class_b)

    return "OK"
}
