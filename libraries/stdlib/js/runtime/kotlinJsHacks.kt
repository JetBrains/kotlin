/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@PublishedApi
internal fun <T : Enum<T>> enumValuesIntrinsic(): Array<T> =
    throw IllegalStateException("Should be replaced by compiler")

@PublishedApi
internal fun <T : Enum<T>> enumValueOfIntrinsic(@Suppress("UNUSED_PARAMETER") name: String): T =
    throw IllegalStateException("Should be replaced by compiler")

// These were necessary for legacy-property-access functionality in IR
// But this functionality is not required anymore, so these methods are redundant
// But they can't be removed because if old version of compiler will compile against new version of stdlib
// (when these methods are removed) compiler will fail with Internal error
@Deprecated("This is an intrinsic for removed functionality", level = DeprecationLevel.HIDDEN)
internal fun safePropertyGet(self: dynamic, getterName: String, propName: String): dynamic {
    val getter = self[getterName]
    return if (getter != null) getter.call(self) else self[propName]
}

@Deprecated("This is an intrinsic for removed functionality", level = DeprecationLevel.HIDDEN)
internal fun safePropertySet(self: dynamic, setterName: String, propName: String, value: dynamic) {
    val setter = self[setterName]
    if (setter != null) setter.call(self, value) else self[propName] = value
}

/**
 * Implements the annotated function in JavaScript.
 * [code] must contain a JS expression that evaluates to JS function with signature that matches the annotated Kotlin function.
 *
 * For example, a function that adds two Doubles:
 *
 * ```kotlin
 * @JsFun("function (x, y) { return x + y; }")
 * external fun jsAdd(x: Double, y: Double): Double
 * ```
 *
 * @property code The JavaScript code
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
internal annotation class JsFun(val code: String)

/**
 * The same as [JsFun], but is intended only for use by the compiler (to be precise, by `JsCodeOutliningLowering`).
 *
 * Unlike [JsFun], this annotation contains the debug information in the Source Map 3 format which maps offsets in [jsFunctionExpression]
 * to the offsets in the original Kotlin file with the [js] call from which this annotation was generated.
 *
 * This annotation was introduced so that we didn't have to consider any compatibility implications of potentially publicizing [JsFun].
 * For that reason, [kotlin.js.JsOutlinedFunction] will forever remain internal.
 */
@Target(AnnotationTarget.FUNCTION)
@PublishedApi
@Suppress("unused") // used by JsCodeOutliningLowering
internal annotation class JsOutlinedFunction(val jsFunctionExpression: String, val sourceMap: String)

/**
 * The annotation is needed for annotating function declarations that should be compiled as ES6 generators
 */
@Target(AnnotationTarget.FUNCTION)
internal annotation class JsGenerator

/**
 * The annotation is needed for annotating class declarations and type alias which are used inside exported declarations, but
 * doesn't contain @JsExport annotation
 * This information is used for generating special tagged types inside d.ts files, for more strict usage of implicitly exported entities
 */
@Target(AnnotationTarget.CLASS)
internal annotation class JsImplicitExport(val couldBeConvertedToExplicitExport: Boolean)

/**
 * The annotation is needed for annotating function declarations that should not accept any dispatch receiver
 * It's used only internally (for now, only in js-plain-object plugin)
 */
@Target(AnnotationTarget.FUNCTION)
@PublishedApi
@Suppress("unused") // used by @JsPlainObject compiler plugin
internal annotation class JsNoDispatchReceiver