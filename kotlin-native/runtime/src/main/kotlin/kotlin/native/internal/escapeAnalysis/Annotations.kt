/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal.escapeAnalysis

/**
 * Escape analysis annotations.
 *
 * Kotlin/Native uses escape analysis in optimized builds to determine which objects can be allocated on the stack.
 * It involves analysing function bodies to determine what happens to objects from the [function signature][FunctionSignature].
 *
 * For `external fun` it is impossible to analyse the function body, because it doesn't have any. Therefore,
 * all `external fun`s with object types in their signature must be correctly annotated by-hand:
 * * Use [PointsTo] to annotate how the signature values right after the function call relate to the signature values
 *   just before the function call.
 * * Use [Escapes] (or [Escapes.Nothing]) to annotate if any of the signature values after the function call have
 *   escaped (e.g. get stored in some global) to the heap.
 *
 * The compiler checks that, apart from a few exceptions, every `external fun` in the stdlib with object types in
 * the function signature is annotated with one of [Escapes], [Escapes.Nothing] or [PointsTo].
 *
 * For more details, see `EscapeAnalysis.kt` in the compiler sources.
 */
@Suppress("unused") // A hack to have a place for file-level documentation
private object EscapeAnalysisAnnotations

/**
 * For escape analysis, function signature is defined as (in order):
 * dispatch receiver, extension receiver, all parameters and the return value.
 *
 * @see EscapeAnalysisAnnotations
 */
@Suppress("unused") // A hack to have a documentation anchor for shared definition.
private object FunctionSignature

/**
 * Specifies which values of the [function signature][FunctionSignature] might have escaped to the heap
 * after the function call (see [EscapeAnalysisAnnotations] for details).
 *
 * ```
 * class C {
 *     @Escapes(0b10101)
 *     external fun Array<Any>.f(p0: Any, p1: Any): Any
 * }
 * ```
 * In this example `f` has 2 parameters, 2 receivers and 1 return value. The bitmask `0b10101` is deciphered as follows:
 * ```
 * 0b1  0  1  0  1
 *   ^  ^  ^  ^  ^
 *   |  |  |  |  |
 *   |  |  |  |  this@C
 *   |  p1 p0 this@Array<Any>
 *   return value
 * ```
 * So, the dispatch receiver `this@C`, `p0` and the return value escape, the extension receiver `this@Array<Any>` and `p1` do not.
 *
 * ```
 * @Escapes(0b1)
 * external fun g(): Any
 * ```
 * And here `g` has no parameters and receivers, but has a return value. Its bitmask is `0b1` which means, that the return value escapes.
 *
 * Return value is considered non-escaping iff there are no other references to it. For example:
 * ```
 * var global = Any()
 * fun foo() = global
 * val globalList = mutableListOf<Any>()
 * fun bar(): Any {
 *     val result = Any()
 *     globalList.add(result)
 *     return result
 * }
 * fun baz() = Any()
 * ```
 * * `foo()`'s return value escapes, because it's referenced by `global`.
 * * `bar()`'s return value escapes, because it's referenced by `globalList`.
 * * `baz()`'s return value does not escape, because there are no other references to it.
 *
 * Non-escaping return value even if it's heap-allocated can be taken advantage of by the escape analysis. For example:
 * ```
 * class A(var x: Any?)
 * fun f(): A = A(null)
 * fun g() {
 *     val a = f()
 *     a.x = Any()
 * }
 * ```
 * `Any()` may be stack-allocated, because it gets stored in `a` that is non-escaping. This works fine, because once we leave `g()`
 * `a` is lost from the root set and so its fields won't be scanned by the GC, and it's fine that `a.x` is garbage.
 * Note: this doesn't work if `A` is marked with [@HasFinalizer][kotlin.native.internal.HasFinalizer], such types must always be
 * heap-allocated.
 *
 * @param who bitmask of the [function signature][FunctionSignature] where set bits indicate escaping
 * @see Escapes.Nothing
 * @see EscapeAnalysisAnnotations
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class Escapes(val who: Int) {

    /**
     * Marks a function where nothing in the [function signature][FunctionSignature] escapes (see [EscapeAnalysisAnnotations] for details).
     *
     * Equivalent to `@Escapes(0)`.
     *
     * @see Escapes
     * @see EscapeAnalysisAnnotations
     */
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.BINARY)
    annotation class Nothing
}

/**
 * Specifies how the values of the [function signature][FunctionSignature] right after the function call relate to
 * the function signature values just before the call (see [EscapeAnalysisAnnotations] for details).
 *
 * There are 4 kinds of "`p1` points to `p2`" (**note**: kinds 2, 3 and 4 are defined recurrently):
 * 1. `p1 -> p2`: after the call `p1` may reference the same object as `p2` from before the call; only the return value may have this kind
 * 2. `p1 -> p2.intestines`: `p1` points (with any kind) to `p4`, where `p4` is some element of `p2`
 * 3. `p1.intestines -> p2`: `p3` points (with any kind) to `p2`, where `p3` is some element of `p1`
 * 4. `p1.intestines -> p2.intestines`: `p3` points (with any kind) to `p4`, where `p3` is some element of `p1` and `p4` is some element of `p2`
 *
 * Kind `0` means, that there's no relation between `p1` after the call and `p2` from before the call.
 *
 * `intestines` is typically used with arrays to mean "elements of the array".
 *
 * [onWhom] will contain the list of lists of nibbles, where each nibble is a kind (and so has value 0-4).
 * The order in both the external and internal lists corresponds to the function signature.
 *
 * ```
 * class C {
 *     @PointsTo(0x00000, 0x03400, 0x00000, 0x00000, 0x01020)
 *     external fun Array<Any>.f(p0: Array<Any>, p1: Any): Any
 * }
 * ```
 * In this example `f` has 2 parameters, 2 receivers and 1 return value.
 * A value `0xabcde` is deciphered as:
 * ```
 * 0xa  b  c  d  e
 *   ^  ^  ^  ^  ^
 *   |  |  |  |  |
 *   |  |  |  |  this@C
 *   |  p1 p0 this@Array<Any>
 *   return value
 * ```
 *
 * So, if `f` was called like:
 * ```
 * val r = arr.f(a0, a1) // with implicit this@C
 * ```
 * The second `0x03400` means that `arr` points to `a0` with kind `4` and to `a1` with kind `3`:
 * * after the call some element of `arr` may point to `a1` (for example, `arr[1] === a1`, or even `arr[1][2] === a1[3]`, or …)
 * * after the call some element of `arr` may point to some element of `a0` (for example, `arr[1] === a0[2]`, but not
 *   `arr[1] === a0` - it should have been kind `3`)
 *
 * And the last `0x01020` means that `r` points to `a1` with kind `1` and to `arr` with kind `2`:
 * * after the call `r` may reference the same object as `a1`
 * * after the call `r` may point to some element of `arr` (for example, `r === arr[1]`, or `r[4] === arr[2][5]`, or …)
 *
 * ```
 * @PointsTo(0x00, 0x01)
 * external fun g(p0: Any): Any
 * ```
 * And for `g` the annotation means that after `val r = g(a0)`, it's possible for `r === a0`.
 *
 * @param onWhom a list corresponding the [function signature][FunctionSignature], where each item `i` is a list of nibbles (packed into `Int`) and each nibble
 *               `j` is a value `0-4` representing a kind of function signature element `i` pointing to function signature element `j`
 * @see EscapeAnalysisAnnotations
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class PointsTo(vararg val onWhom: Int)
