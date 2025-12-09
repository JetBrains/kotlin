// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +JsAllowExportingSuspendFunctions +ContextParameters
// MODULE: JS_TESTS
// FILE: suspend-functions.kt

package foo

private fun assert(condition: Boolean) {
    if (!condition) {
        throw Throwable("Unexpected behavior")
    }
}

@JsExport
suspend fun sum(x: Int, y: Int): Int = x + y

@JsExport
suspend fun varargInt(vararg x: Int): Int = x.size

@JsExport
suspend fun varargNullableInt(vararg x: Int?): Int = x.size

@JsExport
suspend fun varargWithOtherParameters(x: String, vararg y: String, z: String): Int =
    x.length + y.size + z.length

@JsExport
suspend fun varargWithComplexType(vararg x: (Array<IntArray>) -> Array<IntArray>): Int =
    x.size

@JsExport
suspend fun sumNullable(x: Int?, y: Int?): Int =
    (x ?: 0) + (y ?: 0)

@JsExport
suspend fun defaultParameters(a: String, x: Int = 10, y: String = "OK"): String =
    a + x.toString() + y

@JsExport
suspend fun <T> generic1(x: T): T = x

@JsExport
suspend fun <T> generic2(x: T?): Boolean = (x == null)

@JsExport
suspend fun <T : String> genericWithConstraint(x: T): T = x

@JsExport
suspend fun <T> genericWithMultipleConstraints(x: T): T
        where T : Comparable<T>,
              T : SomeExternalInterface,
              T : Throwable = x

@JsExport
@JsName("generic3")
suspend fun <A, B, C, D, E> forth(a: A, b: B, c: C, d: D): E? = null

@JsExport
suspend inline fun inlineFun(x: Int, callback: (Int) -> Int): Int = callback(x)

@JsExport
suspend fun simpleSuspendFun(x: Int) = x

@JsExport
suspend inline fun inlineChain(x: Int) = inlineFun(x) { x -> simpleSuspendFun(x) }

@JsExport
suspend fun Int.suspendExtensionFun() = this

@JsExport
context(ctx: Int)
suspend fun suspendFunWithContext() = ctx

@JsExport
class WithSuspendExtensionFunAndContext {
    context(ctx: Int)
    suspend fun Int.suspendFun() = this + ctx
}

@JsExport
class WithSuspendFunInsideInnerClass {
    inner class Inner {
        suspend fun suspendFun() = 42
    }
}

@JsExport.Ignore
fun suspendParameter(call: suspend () -> Int) = <!ILLEGAL_SUSPEND_FUNCTION_CALL!>call<!>()

@JsExport
external interface SomeExternalInterface

@JsExport
interface HolderOfSum {
    suspend fun sum(x: Int, y: Int): Int
    suspend fun sumNullable(x: Int?, y: Int?): Int
}

@JsExport
open class Test : HolderOfSum {
    override suspend fun sum(x: Int, y: Int): Int =
        x + y

    open suspend fun varargInt(vararg x: Int): Int =
        x.size

    open suspend fun varargNullableInt(vararg x: Int?): Int =
        x.size

    open suspend fun varargWithOtherParameters(x: String, vararg y: String, z: String): Int =
        x.length + y.size + z.length

    open suspend fun varargWithComplexType(vararg x: (Array<IntArray>) -> Array<IntArray>): Int =
        x.size

    override suspend fun sumNullable(x: Int?, y: Int?): Int =
        (x ?: 0) + (y ?: 0)

    open suspend fun defaultParameters(a: String, x: Int = 10, y: String = "OK"): String =
        a + x.toString() + y

    open suspend fun <T> generic1(x: T): T = x

    open suspend fun <T> generic2(x: T?): Boolean = (x == null)

    open suspend fun <T : String> genericWithConstraint(x: T): T = x

    open suspend fun <T> genericWithMultipleConstraints(x: T): T
            where T : Comparable<T>,
                  T : SomeExternalInterface,
                  T : Throwable = x

    @JsName("generic3")
    open suspend fun <A, B, C, D, E> forth(a: A, b: B, c: C, d: D): E? = null
}

@JsExport
open class TestChild : Test() {
    override suspend fun varargInt(vararg x: Int): Int =
        x.size + 2

    override suspend fun sumNullable(x: Int?, y: Int?): Int =
        (x ?: 1) + (y ?: 1)

    override suspend fun <A, B, C, D, E> forth(a: A, b: B, c: C, d: D): E? = js("'OK'").unsafeCast<E>()
}

@JsExport.Ignore
open class NotExportedTestChild : Test() {
    override suspend fun sum(x: Int, y: Int): Int = 42
    override suspend fun varargNullableInt(vararg x: Int?): Int = 43
    override suspend fun sumNullable(x: Int?, y: Int?): Int = 44
    override suspend fun <A, B, C, D, E> forth(a: A, b: B, c: C, d: D): E? = js("'45'").unsafeCast<E>()
}

@JsExport
fun generateOneMoreChildOfTest(): Test = NotExportedTestChild()

@JsExport
suspend fun acceptHolderOfSum(test: HolderOfSum) {
    assert(test.sum(1, 2) == when (test) {
        is NotExportedTestChild -> 42
        else -> 3
    })

    assert(test.sumNullable(null, 5) == when (test) {
        is TestChild -> 6
        is NotExportedTestChild -> 44
        else -> 5
    })
}

@JsExport
suspend fun acceptTest(test: Test) {
    assert(test.sum(1, 2) == when (test) {
        is NotExportedTestChild -> 42
        else -> 3
    })

    assert(test.varargInt(1, 2, 3) == when (test::class.js) {
        TestChild::class.js -> 5
        Test::class.js, NotExportedTestChild::class.js  -> 3
        else /* TypeScript */ -> 5
    })

    assert(test.varargNullableInt(1, null, 3) == when (test) {
        is NotExportedTestChild -> 43
        else -> 3
    })

    assert(test.varargWithOtherParameters(x = "start", "a", "b", z = "end") == 10)
    assert(test.varargWithComplexType({ it -> it }) == 1)
    assert(test.sumNullable(null, 5) == when (test) {
        is TestChild -> 6
        is NotExportedTestChild -> 44
        else -> 5
    })

    assert(test.defaultParameters("test") == when (test::class.js) {
        TestChild::class.js, Test::class.js, NotExportedTestChild::class.js -> "test10OK"
        else /* TypeScript */ -> "OK"
    })
    assert(test.defaultParameters("test", 20) == when (test::class.js) {
        TestChild::class.js, Test::class.js, NotExportedTestChild::class.js -> "test20OK"
        else /* TypeScript */ -> "\u0014K"
    })
    assert(test.defaultParameters("test", 20, "custom") == when (test::class.js) {
        TestChild::class.js, Test::class.js, NotExportedTestChild::class.js -> "test20custom"
        else /* TypeScript */ -> "\u0014custom"
    })

    assert(test.generic1("string") == "string")

    assert(test.generic2<String>(null) == when (test::class.js) {
        TestChild::class.js, Test::class.js, NotExportedTestChild::class.js -> true
        else /* TypeScript */ -> false
    })

    assert(test.genericWithConstraint("constrained") == "constrained")
    val result = test.forth<Int, String, Boolean, Double, String>(1, "test", true, 1.0)
    assert(result == when (test) {
        is TestChild -> "OK"
        is NotExportedTestChild -> "45"
        else -> null
    })

    // Create custom error type that satisfies all constraints
    class CustomError(message: String) : Error(message), SomeExternalInterface, Comparable<CustomError> {
        override fun compareTo(other: CustomError): Int = 0
    }

    val error = CustomError("test")
    assert(test.genericWithMultipleConstraints(error) == error)
}

@JsExport.Ignore
open class NotExportedParent {
    open suspend fun parentSuspendFun1() = "NotExportedParent 1"
    open suspend fun parentSuspendFun2() = "NotExportedParent 2"
}

@JsExport
class ExportedChild : NotExportedParent() {
    override suspend fun parentSuspendFun2() = "ExportedChild 2"
    suspend fun childSuspendFun() = "ExportedChild"
}

@JsExport
suspend fun acceptExportedChild(child: ExportedChild) {
    val parent = NotExportedParent()

    assert(parent.parentSuspendFun1() == "NotExportedParent 1")
    assert(parent.parentSuspendFun2() == "NotExportedParent 2")

    assert(child.parentSuspendFun1() == "NotExportedParent 1")
    assert(child.parentSuspendFun2() == "ExportedChild 2")

    assert(child.childSuspendFun() == when (child::class.js) {
        ExportedChild::class.js -> "ExportedChild"
        else /* TypeScript */ -> "TypeScriptChild"
    })
}