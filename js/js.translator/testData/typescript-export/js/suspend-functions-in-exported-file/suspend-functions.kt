// This file was generated automatically. See  generateTestDataForTypeScriptWithFileExport.kt
// DO NOT MODIFY IT MANUALLY.

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +JsExportSuspendFunctions
// MODULE: JS_TESTS
// FILE: suspend-functions.kt

@file:JsExport

package foo

private fun assert(condition: Boolean) {
    if (!condition) {
        throw Throwable("Unexpected behavior")
    }
}


suspend fun sum(x: Int, y: Int): Int = x + y


suspend fun varargInt(vararg x: Int): Int = x.size


suspend fun varargNullableInt(vararg x: Int?): Int = x.size


suspend fun varargWithOtherParameters(x: String, vararg y: String, z: String): Int =
    x.length + y.size + z.length


suspend fun varargWithComplexType(vararg x: (Array<IntArray>) -> Array<IntArray>): Int =
    x.size


suspend fun sumNullable(x: Int?, y: Int?): Int =
    (x ?: 0) + (y ?: 0)


suspend fun defaultParameters(a: String, x: Int = 10, y: String = "OK"): String =
    a + x.toString() + y


suspend fun <T> generic1(x: T): T = x


suspend fun <T> generic2(x: T?): Boolean = (x == null)


suspend fun <T : String> genericWithConstraint(x: T): T = x


suspend fun <T> genericWithMultipleConstraints(x: T): T
        where T : Comparable<T>,
              T : SomeExternalInterface,
              T : Throwable = x


@JsName("generic3")
suspend fun <A, B, C, D, E> forth(a: A, b: B, c: C, d: D): E? = null


suspend inline fun inlineFun(x: Int, callback: (Int) -> Unit) {
    callback(x)
}


external interface SomeExternalInterface


interface HolderOfSum {
    suspend fun sum(x: Int, y: Int): Int
    suspend fun sumNullable(x: Int?, y: Int?): Int
}


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


open class TestChild : Test() {
    override suspend fun varargInt(vararg x: Int): Int =
        x.size + 2

    override suspend fun sumNullable(x: Int?, y: Int?): Int =
        (x ?: 1) + (y ?: 1)

    override suspend fun <A, B, C, D, E> forth(a: A, b: B, c: C, d: D): E? = js("'OK'").unsafeCast<E>()
}

@JsExport suspend fun acceptTest(test: Test) {
    assert(test.sum(1, 2) == 3)
    assert(test.varargInt(1, 2, 3) == when (test::class.js) {
        TestChild::class.js -> 5
        Test::class.js -> 3
        else /* TypeScript */ -> 5
    })
    assert(test.varargNullableInt(1, null, 3) == 3)
    assert(test.varargWithOtherParameters(x = "start", "a", "b", z = "end") == 10)
    assert(test.varargWithComplexType({ it -> it }) == 1)
    assert(test.sumNullable(null, 5) == when (test) {
        is TestChild -> 6
        else -> 5
    })

    assert(test.defaultParameters("test") == when (test::class.js) {
        TestChild::class.js, Test::class.js -> "test10OK"
        else /* TypeScript */ -> "OK"
    })
    assert(test.defaultParameters("test", 20) == when (test::class.js) {
        TestChild::class.js, Test::class.js -> "test20OK"
        else /* TypeScript */ -> "\u0014K"
    })
    assert(test.defaultParameters("test", 20, "custom") == when (test::class.js) {
        TestChild::class.js, Test::class.js -> "test20custom"
        else /* TypeScript */ -> "\u0014custom"
    })

    assert(test.generic1("string") == "string")

    assert(test.generic2<String>(null) == when (test::class.js) {
        TestChild::class.js, Test::class.js -> true
        else /* TypeScript */ -> false
    })

    assert(test.genericWithConstraint("constrained") == "constrained")
    val result = test.forth<Int, String, Boolean, Double, String>(1, "test", true, 1.0)
    assert(when (test) {
        is TestChild -> result is String
        else -> result == null
    })

    // Create custom error type that satisfies all constraints
    class CustomError(message: String) : Error(message), SomeExternalInterface, Comparable<CustomError> {
        override fun compareTo(other: CustomError): Int = 0
    }

    val error = CustomError("test")
    assert(test.genericWithMultipleConstraints(error) == error)
}