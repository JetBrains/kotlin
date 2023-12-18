/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NON_MEMBER_FUNCTION_NO_BODY", "UNUSED_PARAMETER", "unused")

package kotlin.js

@RequiresOptIn(message = "Here be dragons! This is a compiler intrinsic, proceed with care!")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
internal annotation class JsIntrinsic

@JsIntrinsic
internal fun jsEqeq(a: Any?, b: Any?): Boolean

@JsIntrinsic
internal fun jsNotEq(a: Any?, b: Any?): Boolean

@JsIntrinsic
internal fun jsUndefined(): Nothing?

@JsIntrinsic
internal fun jsEqeqeq(a: Any?, b: Any?): Boolean

@JsIntrinsic
internal fun jsNotEqeq(a: Any?, b: Any?): Boolean

@JsIntrinsic
internal fun jsGt(a: Any?, b: Any?): Boolean

@JsIntrinsic
internal fun jsGtEq(a: Any?, b: Any?): Boolean

@JsIntrinsic
internal fun jsLt(a: Any?, b: Any?): Boolean

@JsIntrinsic
internal fun jsLtEq(a: Any?, b: Any?): Boolean

@JsIntrinsic
internal fun jsNot(a: Any?): Boolean

@JsIntrinsic
internal fun jsUnaryPlus(a: Any?): Any?

@JsIntrinsic
internal fun jsUnaryMinus(a: Any?): Any?

@JsIntrinsic
internal fun jsPrefixInc(a: Any?): Any?

@JsIntrinsic
internal fun jsPostfixInc(a: Any?): Any?

@JsIntrinsic
internal fun jsPrefixDec(a: Any?): Any?

@JsIntrinsic
internal fun jsPostfixDec(a: Any?): Any?

@JsIntrinsic
internal fun jsPlus(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsMinus(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsMult(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsDiv(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsMod(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsPlusAssign(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsMinusAssign(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsMultAssign(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsDivAssign(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsModAssign(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsAnd(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsOr(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsBitAnd(a: Any?, b: Any?): Int

@JsIntrinsic
internal fun jsBitOr(a: Any?, b: Any?): Int

@JsIntrinsic
internal fun jsBitXor(a: Any?, b: Any?): Int

@JsIntrinsic
internal fun jsBitNot(a: Any?): Int

@JsIntrinsic
internal fun jsBitShiftR(a: Any?, b: Any?): Int

@JsIntrinsic
internal fun jsBitShiftRU(a: Any?, b: Any?): Int

@JsIntrinsic
internal fun jsBitShiftL(a: Any?, b: Any?): Int

@JsIntrinsic
internal fun jsInstanceOfIntrinsic(a: Any?, b: Any?): Boolean

@JsIntrinsic
internal fun jsNewTarget(a: Any?): Any?

@JsIntrinsic
internal fun emptyObject(a: Any?): Any?

@JsIntrinsic
internal fun openInitializerBox(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsArrayLength(a: Any?): Any?

@JsIntrinsic
internal fun jsArrayGet(a: Any?, b: Any?): Any?

@JsIntrinsic
internal fun jsArraySet(a: Any?, b: Any?, c: Any?): Any?

@JsIntrinsic
internal fun arrayLiteral(a: Any?): Any?

@JsIntrinsic
internal fun int8Array(a: Any?): Any?

@JsIntrinsic
internal fun int16Array(a: Any?): Any?

@JsIntrinsic
internal fun int32Array(a: Any?): Any?

@JsIntrinsic
internal fun float32Array(a: Any?): Any?

@JsIntrinsic
internal fun float64Array(a: Any?): Any?

@JsIntrinsic
internal fun int8ArrayOf(a: Any?): Any?

@JsIntrinsic
internal fun int16ArrayOf(a: Any?): Any?

@JsIntrinsic
internal fun int32ArrayOf(a: Any?): Any?

@JsIntrinsic
internal fun float32ArrayOf(a: Any?): Any?

@JsIntrinsic
internal fun float64ArrayOf(a: Any?): Any?

@JsIntrinsic
internal fun <T> sharedBoxCreate(v: T?): dynamic

@JsIntrinsic
internal fun <T> sharedBoxRead(box: dynamic): T?

@JsIntrinsic
internal fun <T> sharedBoxWrite(box: dynamic, nv: T?)

@JsIntrinsic
internal fun <T> DefaultType(): T

@JsIntrinsic
internal fun jsBind(receiver: Any?, target: Any?): Any?

@JsIntrinsic
internal fun jsCall(receiver: Any?, target: Any?, vararg args: Any?): Any?

@JsIntrinsic
internal fun <A> slice(a: A): A

@JsIntrinsic
internal fun <T> jsArrayLike2Array(arrayLike: Any?): Array<T>

@JsIntrinsic
internal fun <T> jsSliceArrayLikeFromIndex(arrayLike: Any?, start: Int): Array<T>

@JsIntrinsic
internal fun <T> jsSliceArrayLikeFromIndexToIndex(arrayLike: Any?, start: Int, end: Int): Array<T>

@JsIntrinsic
internal fun unreachable(): Nothing

@JsIntrinsic
internal fun jsArguments(): Any?

@JsIntrinsic
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
internal fun <reified T : Any> jsNewAnonymousClass(superClass: JsClass<T>): JsClass<T>

@JsIntrinsic
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE") // TODO: mark `inline` and skip in inliner
internal fun <reified T : Any> jsClassIntrinsic(): JsClass<T>

// Returns true if the specified property is in the specified object or its prototype chain.
@JsIntrinsic
internal fun jsInIntrinsic(lhs: Any?, rhs: Any): Boolean

@JsIntrinsic
internal fun jsDelete(e: Any?)

@JsIntrinsic
internal fun jsContextfulRef(context: dynamic, fn: dynamic): dynamic

@JsIntrinsic
internal fun jsIsEs6(): Boolean

@JsIntrinsic
internal fun <T> jsYield(suspendFunction: () -> T): T
