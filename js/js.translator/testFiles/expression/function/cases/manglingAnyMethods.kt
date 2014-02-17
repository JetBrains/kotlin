package foo

public fun equals(a: Any?): Boolean = true
public fun hashCode(): Int = 0
public fun toString(): String = ""

public class PublicClassWithPublics {
    public fun equals(a: Any?): Boolean = this.identityEquals(a)
    public fun hashCode(): Int = 0
    public fun toString(): String = "PublicClass"
}

class InternalClassWithPublics {
    public fun equals(a: Any?): Boolean = this.identityEquals(a)
    public fun hashCode(): Int = 1
    public fun toString(): String = "InternalClass"

    // overloads
    public fun equals(a: Any?, b: Any?): Boolean = a == b
    public fun hashCode(i: Int): Int = i
    public fun toString(s: String): String = s
}

private class PrivateClassWithPublics {
    public fun equals(a: Any?): Boolean = this.identityEquals(a)
    public fun hashCode(): Int = 2
    public fun toString(): String = "InternalClass"

    // overloads
    public fun equals(a: Any?, b: Any?): Boolean = a == b
    public fun hashCode(i: Int): Int = i
    public fun toString(s: String): String = s
}

public class PublicClassWithProtecteds {
    protected fun equals(a: Any?): Boolean = this.identityEquals(a)
    protected fun hashCode(): Int = 0
    protected fun toString(): String = "public Bar"

    val call_equals: ()->Unit = { equals(0) }
    val call_hashCode: ()->Unit = { hashCode() }
    val call_toString: ()->Unit = { toString() }
}

class InternalClassWithProtecteds {
    protected fun equals(a: Any?): Boolean = this.identityEquals(a)
    protected fun hashCode(): Int = 1
    protected fun toString(): String = "InternalClass"

    // overloads
    protected fun equals(a: Any?, b: Any?): Boolean = a == b
    protected fun hashCode(i: Int): Int = i
    protected fun toString(s: String): String = s

    val call_equals: ()->Unit = { equals(0) }
    val call_hashCode: ()->Unit = { hashCode() }
    val call_toString: ()->Unit = { toString() }
    val call_equals2: ()->Unit = { equals(0, 1) }
    val call_hashCode2: ()->Unit = { hashCode(2) }
    val call_toString2: ()->Unit = { toString("3") }
}

private class PrivateClassWithProtecteds {
    protected fun equals(a: Any?): Boolean = this.identityEquals(a)
    protected fun hashCode(): Int = 2
    protected fun toString(): String = "InternalClass"

    // overloads
    protected fun equals(a: Any?, b: Any?): Boolean = a == b
    protected fun hashCode(i: Int): Int = i
    protected fun toString(s: String): String = s

    val call_equals: ()->Unit = { equals(0) }
    val call_hashCode: ()->Unit = { hashCode() }
    val call_toString: ()->Unit = { toString() }
    val call_equals2: ()->Unit = { equals(0, 1) }
    val call_hashCode2: ()->Unit = { hashCode(2) }
    val call_toString2: ()->Unit = { toString("3") }
}

public class PublicClassWithInternals {
    fun equals(a: Any?): Boolean = this.identityEquals(a)
    fun hashCode(): Int = 0
    fun toString(): String = "public Bar"
}

class InternalClassWithInternals {
    fun equals(a: Any?): Boolean = this.identityEquals(a)
    fun hashCode(): Int = 1
    fun toString(): String = "InternalClass"

    // overloads
    fun equals(a: Any?, b: Any?): Boolean = a == b
    fun hashCode(i: Int): Int = i
    fun toString(s: String): String = s
}

private class PrivateClassWithInternals {
    fun equals(a: Any?): Boolean = this.identityEquals(a)
    fun hashCode(): Int = 2
    fun toString(): String = "InternalClass"

    // overloads
    fun equals(a: Any?, b: Any?): Boolean = a == b
    fun hashCode(i: Int): Int = i
    fun toString(s: String): String = s
}

public class PublicClassWithPrivates {
    private fun equals(a: Any?): Boolean = this.identityEquals(a)
    private fun hashCode(): Int = 0
    private fun toString(): String = "public Bar"

    val call_equals: ()->Unit = { equals(0) }
    val call_hashCode: ()->Unit = { hashCode() }
    val call_toString: ()->Unit = { toString() }
}

class InternalClassWithPrivates {
    private fun equals(a: Any?): Boolean = this.identityEquals(a)
    private fun hashCode(): Int = 1
    private fun toString(): String = "InternalClass"

    // overloads
    private fun equals(a: Any?, b: Any?): Boolean = a == b
    private fun hashCode(i: Int): Int = i
    private fun toString(s: String): String = s

    val call_equals: ()->Unit = { equals(0) }
    val call_hashCode: ()->Unit = { hashCode() }
    val call_toString: ()->Unit = { toString() }
    val call_equals2: ()->Unit = { equals(0, 1) }
    val call_hashCode2: ()->Unit = { hashCode(2) }
    val call_toString2: ()->Unit = { toString("3") }
}

private class PrivateClassWithPrivates {
    private fun equals(a: Any?): Boolean = this.identityEquals(a)
    private fun hashCode(): Int = 2
    private fun toString(): String = "InternalClass"

    // overloads
    private fun equals(a: Any?, b: Any?): Boolean = a == b
    private fun hashCode(i: Int): Int = i
    private fun toString(s: String): String = s

    val call_equals: ()->Unit = { equals(0) }
    val call_hashCode: ()->Unit = { hashCode() }
    val call_toString: ()->Unit = { toString() }
    val call_equals2: ()->Unit = { equals(0, 1) }
    val call_hashCode2: ()->Unit = { hashCode(2) }
    val call_toString2: ()->Unit = { toString("3") }
}

// Helpers

native
fun String.search(regexp: RegExp): Int = noImpl

native
class RegExp(regexp: String, flags: String = "") {
    fun exec(s: String): Array<String>? = noImpl
}

val CALEE_NAME = RegExp("""((?:equals|hashCode|toString)[^(]*)""")

fun <T> Function0<T>.extractNames(): Array<String> {
    val names = CALEE_NAME.exec(this.toString(): String)

    if (names == null || names.size != 2) {
        throw Exception("Cannot extract function name, $names for actual = \"$this\"")
    }

    return names
}

// Testing

var testGroup = ""

fun test(expected: String, f: ()->Unit){
    val actual = f.extractNames()

    if (expected != actual[1]) {
        throw Exception("Failed on '$testGroup' group: expected = \"$expected\", actual[1] = \"${actual[1]}\"\n actual = $actual")
    }
}

val SIMPLE_EQUALS = "equals"
val SIMPLE_HASH_CODE = "hashCode"
val SIMPLE_TO_STRING = "toString"
val STABLE_EQUALS = { equals(0) }.extractNames()[1]
val STABLE_HASH_CODE = { hashCode()}.extractNames()[1]
val STABLE_TO_STRING = { toString()}.extractNames()[1]

fun box(): String {
    testGroup = "Public Class with publics"
    test(STABLE_EQUALS) { PublicClassWithPublics().equals(0) }
    test(STABLE_HASH_CODE) { PublicClassWithPublics().hashCode() }
    test(STABLE_TO_STRING) { PublicClassWithPublics().toString() }

    testGroup = "Internal Class with publics"
    test(STABLE_EQUALS) { InternalClassWithPublics().equals(0) }
    test(STABLE_HASH_CODE) { InternalClassWithPublics().hashCode() }
    test(STABLE_TO_STRING) { InternalClassWithPublics().toString() }
    //test(SIMPLE_EQUALS) { InternalClassWithPublics().equals(0, 1) }
    //test(SIMPLE_HASH_CODE) { InternalClassWithPublics().hashCode(2) }
    //test(SIMPLE_TO_STRING) { InternalClassWithPublics().toString("3") }

    testGroup = "Private Class with publics"
    test(STABLE_EQUALS) { PrivateClassWithPublics().equals(0) }
    test(STABLE_HASH_CODE) { PrivateClassWithPublics().hashCode() }
    test(STABLE_TO_STRING) { PrivateClassWithPublics().toString() }
    //test(SIMPLE_EQUALS) { PrivateClassWithPublics().equals(0, 1) }
    //test(SIMPLE_HASH_CODE) { PrivateClassWithPublics().hashCode(2) }
    //test(SIMPLE_TO_STRING) { PrivateClassWithPublics().toString("3") }

    testGroup = "Public Class with protecteds"
    test(STABLE_EQUALS, PublicClassWithProtecteds().call_equals)
    test(STABLE_HASH_CODE, PublicClassWithProtecteds().call_hashCode)
    test(STABLE_TO_STRING, PublicClassWithProtecteds().call_toString)

    testGroup = "Internal Class with protecteds"
    test(STABLE_EQUALS, InternalClassWithProtecteds().call_equals)
    test(STABLE_HASH_CODE, InternalClassWithProtecteds().call_hashCode)
    test(STABLE_TO_STRING, InternalClassWithProtecteds().call_toString)
    //test(SIMPLE_EQUALS, InternalClassWithProtecteds().call_equals2)
    //test(SIMPLE_HASH_CODE, InternalClassWithProtecteds().call_hashCode2)
    //test(SIMPLE_TO_STRING, InternalClassWithProtecteds().call_toString2)

    testGroup = "Private Class with protecteds"
    test(STABLE_EQUALS, PrivateClassWithProtecteds().call_equals)
    test(STABLE_HASH_CODE, PrivateClassWithProtecteds().call_hashCode)
    test(STABLE_TO_STRING, PrivateClassWithProtecteds().call_toString)
    //test(STABLE_EQUALS_OVERLOAD, PrivateClassWithProtecteds().call_equals2)
    //test(STABLE_HASH_CODE_OVERLOAD, PrivateClassWithProtecteds().call_hashCode2)
    //test(STABLE_TO_STRING_OVERLOAD, PrivateClassWithProtecteds().call_toString2)

    testGroup = "Public Class with internals"
    test(STABLE_EQUALS) { PublicClassWithInternals().equals(0) }
    test(STABLE_HASH_CODE) { PublicClassWithInternals().hashCode() }
    test(STABLE_TO_STRING) { PublicClassWithInternals().toString() }

    testGroup = "Internal Class with internals"
    test(STABLE_EQUALS) { InternalClassWithInternals().equals(0) }
    test(STABLE_HASH_CODE) { InternalClassWithInternals().hashCode() }
    test(STABLE_TO_STRING) { InternalClassWithInternals().toString() }
    //test(SIMPLE_EQUALS) { InternalClassWithInternals().equals(0, 1) }
    //test(SIMPLE_HASH_CODE) { InternalClassWithInternals().hashCode(2) }
    //test(SIMPLE_TO_STRING) { InternalClassWithInternals().toString("3") }

    testGroup = "Private Class with internals"
    test(STABLE_EQUALS) { PrivateClassWithInternals().equals(0) }
    test(STABLE_HASH_CODE) { PrivateClassWithInternals().hashCode() }
    test(STABLE_TO_STRING) { PrivateClassWithInternals().toString() }
    //test(SIMPLE_EQUALS) { PrivateClassWithInternals().equals(0, 1) }
    //test(SIMPLE_HASH_CODE) { PrivateClassWithInternals().hashCode(2) }
    //test(SIMPLE_TO_STRING) { PrivateClassWithInternals().toString("3") }

    testGroup = "Public Class with privates"
    test(STABLE_EQUALS, PublicClassWithPrivates().call_equals)
    test(STABLE_HASH_CODE, PublicClassWithPrivates().call_hashCode)
    test(STABLE_TO_STRING, PublicClassWithPrivates().call_toString)

    testGroup = "Internal Class with privates"
    test(STABLE_EQUALS, InternalClassWithPrivates().call_equals)
    test(STABLE_HASH_CODE, InternalClassWithPrivates().call_hashCode)
    test(STABLE_TO_STRING, InternalClassWithPrivates().call_toString)
    //test(SIMPLE_EQUALS, InternalClassWithPrivates().call_equals2)
    //test(SIMPLE_HASH_CODE, InternalClassWithPrivates().call_hashCode2)
    //test(SIMPLE_TO_STRING, InternalClassWithPrivates().call_toString2)

    testGroup = "Private Class with privates"
    test(STABLE_EQUALS, PrivateClassWithPrivates().call_equals)
    test(STABLE_HASH_CODE, PrivateClassWithPrivates().call_hashCode)
    test(STABLE_TO_STRING, PrivateClassWithPrivates().call_toString)
    //test(SIMPLE_EQUALS, PrivateClassWithPrivates().call_equals2)
    //test(SIMPLE_HASH_CODE, PrivateClassWithPrivates().call_hashCode2)
    //test(SIMPLE_TO_STRING, PrivateClassWithPrivates().call_toString2)

    return "OK"
}
