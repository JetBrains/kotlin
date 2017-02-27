package kotlin.js

@Deprecated(message = "Use `definedExternally` instead", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("definedExternally"))
public external val noImpl: Nothing

/**
 * The property that can be used as a placeholder for statements and values that are defined in JavaScript.
 *
 * This property can be used in two cases:
 *
 *   * To represent body of an external function. In most cases Kotlin does not require to provide bodies of external
 *     functions and properties, but if for some reason you want to (for example, due to limitation of your coding style guides),
 *     you should use `definedExternally`.
 *   * To represent value of default argument.
 *
 * There's two forms of using `definedExternally`:
 *
 *   1. `= definedExternally` (for functions, properties and parameters).
 *   2. `{ definedExternally }` (for functions and property getters/setters).
 *
 * This property can't be used from normal code.
 *
 * Examples:
 *
 * ``` kotlin
 * external fun foo(): String = definedExternally
 * external fun bar(x: Int) { definedExternally }
 * external fun baz(z: Any = definedExternally): Array<Any>
 * external val prop: Float = definedExternally
 * ```
 */
public external val definedExternally: Nothing

/**
 * Exposes the JavaScript [eval function](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval) to Kotlin.
 */
public external fun eval(expr: String): dynamic

/**
 * Exposes the JavaScript [undefined property](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/undefined) to Kotlin.
 */
public external val undefined: Nothing?

@Deprecated("Use toInt() instead.", ReplaceWith("s.toInt()"), level = DeprecationLevel.ERROR)
public external fun parseInt(s: String): Int

@Deprecated("Use toInt(radix) instead.", ReplaceWith("s.toInt(radix)"), level = DeprecationLevel.ERROR)
public external fun parseInt(s: String, radix: Int = definedExternally): Int

@Deprecated("Use toDouble() instead.", ReplaceWith("s.toDouble()"), level = DeprecationLevel.ERROR)
public external fun parseFloat(s: String, radix: Int = definedExternally): Double

/**
 * Puts the given piece of a JavaScript code right into the calling function.
 * The compiler replaces call to `js(...)` code with the string constant provided as a parameter.
 *
 * Example:
 *
 * ``` kotlin
 * fun logToConsole(message: String): Unit {
 *     js("console.log(message)")
 * }
 * ```
 *
 * @param code the piece of JavaScript code to put to the generated code.
 *        Must be a compile-time constant, otherwise compiler produces error message.
 *        You can safely refer to local variables of calling function (but not to local variables of outer functions),
 *        including parameters. You can't refer to functions, properties and classes by their short names.
 */
public external fun js(code: String): dynamic

/**
 * Function corresponding to JavaScript's `typeof` operator
 */
@kotlin.internal.InlineOnly
public inline fun jsTypeOf(a: Any?): String = js("typeof a")

@kotlin.internal.InlineOnly
internal inline fun deleteProperty(obj: Any, property: Any) {
    js("delete obj[property]")
}