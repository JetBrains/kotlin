// DONT_TARGET_EXACT_BACKEND: WASM_JS
// WASM_MUTE_REASON: UNSUPPORTED_JS_INTEROP
// LANGUAGE: +JsAllowExportingValueClasses

// This test file verifies the boxing rules for value classes when instances of those classes are passed
// through language boundaries in external declarations.

package foo

var numberOfConstructorCalls = 0

value class NotExported(val value: Int) {
   init {
       numberOfConstructorCalls++
   }
}

@JsExport
value class Exported(val value: Int) {
    init {
        numberOfConstructorCalls++
    }
}

external fun isUnwrapped(value: NotExported): Boolean
external fun isUnwrapped(value: Exported): Boolean

external fun getNotExported(value: NotExported): NotExported
external fun getExported(value: Exported): Exported

external fun isWrapped(value: Any): Boolean

external interface ContainingAllTheMethods {
    @JsName("isUnwrappedNotExported")
    fun isUnwrapped(value: NotExported): Boolean

    @JsName("isUnwrappedExported")
    fun isUnwrapped(value: Exported): Boolean

    fun getNotExported(value: NotExported): NotExported
    fun getExported(value: Exported): Exported

    fun isWrapped(value: Any): Boolean
}

class ContainingAllTheMethodsImpl : ContainingAllTheMethods {
    override fun isUnwrapped(value: NotExported): Boolean =
        foo.isUnwrapped(value)

    override fun isUnwrapped(value: Exported): Boolean =
        foo.isUnwrapped(value)

    override fun getNotExported(value: NotExported): NotExported =
        foo.getNotExported(value)

    override fun getExported(value: Exported): Exported =
        foo.getExported(value)

    override fun isWrapped(value: Any): Boolean =
        foo.isWrapped(value)
}

fun box(): String {
    val methods = ContainingAllTheMethodsImpl()

    val notExported = NotExported(1)
    val exported = Exported(2)

    if (!isUnwrapped(notExported)) return "fail: not exported is provided as boxed value"
    if (!isUnwrapped(exported)) return "fail: exported is provided as boxed value"
    if (!methods.isUnwrapped(notExported)) return "fail: not exported is provided as boxed value in an overridden method"
    if (!methods.isUnwrapped(exported)) return "fail: exported is provided as boxed value in an overridden method"

    if (!isWrapped(notExported)) return "fail: not exported is provided as raw value"
    if (!isWrapped(exported)) return "fail: exported is provided as raw value"
    if (!methods.isWrapped(notExported)) return "fail: not exported is provided as raw value in an overridden method"
    if (!methods.isWrapped(exported)) return "fail: exported is provided as raw value in an overridden method"

    val returnedNotExported = getNotExported(notExported)
    val returnedExported = getExported(exported)

    if (!isUnwrapped(returnedNotExported)) return "fail: returned not exported from external function is provided as boxed value"
    if (!isUnwrapped(returnedExported)) return "fail: returned exported from external function is provided as boxed value"
    if (!methods.isUnwrapped(returnedNotExported)) return "fail: returned not exported from external function is provided as boxed value in an overridden method"
    if (!methods.isUnwrapped(returnedExported)) return "fail: returned exported from external function is provided as boxed value in an overridden method"

    if (!isWrapped(returnedNotExported)) return "fail: returned not exported from external function is provided as raw value"
    if (!isWrapped(returnedExported)) return "fail: returned exported from external function is provided as raw value"
    if (!methods.isWrapped(returnedNotExported)) return "fail: returned not exported from external function is provided as raw value in an overridden method"
    if (!methods.isWrapped(returnedExported)) return "fail: returned exported from external function is provided as raw value in an overridden method"

    val returnedNotExportedByMethod = methods.getNotExported(notExported)
    val returnedExportedByMethod = methods.getExported(exported)

    if (!isUnwrapped(returnedNotExportedByMethod)) return "fail: returned not exported from overridden method is provided as boxed value"
    if (!isUnwrapped(returnedExportedByMethod)) return "fail: returned exported from overridden method is provided as boxed value"
    if (!methods.isUnwrapped(returnedNotExportedByMethod)) return "fail: returned not exported from overridden method is provided as boxed value in an overridden method"
    if (!methods.isUnwrapped(returnedExportedByMethod)) return "fail: returned exported from overridden method is provided as boxed value in an overridden method"

    if (!isWrapped(returnedNotExportedByMethod)) return "fail: returned not exported from overridden method is provided as raw value"
    if (!isWrapped(returnedExportedByMethod)) return "fail: returned exported from overridden method is provided as raw value"
    if (!methods.isWrapped(returnedNotExportedByMethod)) return "fail: returned not exported from overridden method is provided as raw value in an overridden method"
    if (!methods.isWrapped(returnedExportedByMethod)) return "fail: returned exported from overridden method is provided as raw value in an overridden method"

    if (numberOfConstructorCalls != 2) return "fail: constructors were called $numberOfConstructorCalls times instead of 2"

    return "OK"
}
