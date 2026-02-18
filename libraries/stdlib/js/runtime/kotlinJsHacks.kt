/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.UsedFromCompilerGeneratedCode

@PublishedApi
@UsedFromCompilerGeneratedCode
internal fun <T : Enum<T>> enumValuesIntrinsic(): Array<T> =
    throw IllegalStateException("Should be replaced by compiler")

@PublishedApi
@UsedFromCompilerGeneratedCode
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
@UsedFromCompilerGeneratedCode
internal annotation class JsOutlinedFunction(val jsFunctionExpression: String, val sourceMap: String)

/**
 * The annotation is needed for annotating function declarations that should not accept any dispatch receiver
 * It's used only internally (for now, only in js-plain-object plugin)
 */
@Target(AnnotationTarget.FUNCTION)
@PublishedApi
@Suppress("unused") // used by @JsPlainObject compiler plugin
internal annotation class JsNoDispatchReceiver
