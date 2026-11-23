/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")  // Used by compiler

package kotlin.wasm.internal

import kotlin.internal.UsedFromCompilerGeneratedCode

// TODO maybe just specialize to Int, if generics are problematic
// TODO one problem is that this global will exist even if we don't want to use the shadow stack lowering, i.e. use a few bytes. probably acceptable?
internal class WasmSinglyLinkedList<T>(val elem: T? = null, val next: WasmSinglyLinkedList<T>? = null)

internal var globalShadowStack = WasmSinglyLinkedList<Int>()

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmGetCurrentFunctionFqNameStringLiteralId(): Int /* TODO maybe long? */ =
    implementedAsIntrinsic

@ExcludedFromCodegen
@Suppress("UNUSED_PARAMETER")
@UsedFromCompilerGeneratedCode
internal fun wasmGetStringLiteralForStringLiteralId(id: Int): String =
    implementedAsIntrinsic

// TODO more intrinsics for location retrieval

@Suppress("NOTHING_TO_INLINE")
@UsedFromCompilerGeneratedCode
internal inline fun wasmShadowStackPush() {
    val previousShadowStack = globalShadowStack
    globalShadowStack = WasmSinglyLinkedList(elem = wasmGetCurrentFunctionFqNameStringLiteralId(), next = previousShadowStack)
    wasmShadowStackSaveGlobalToLocal(globalShadowStack)
}


@Suppress("NOTHING_TO_INLINE")
@UsedFromCompilerGeneratedCode
internal inline fun wasmShadowStackPop() {
    assert(globalShadowStack.next != null) { "Cannot pop from shadow stack: shadow stack is empty" }
    globalShadowStack = globalShadowStack.next!!
} // TODO decide whether we want to try to use the local backup for this


// TODO probably all these intrinsics are overkill, lets see

// TODO could also do an array, and save the index as the local?

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmShadowStackSaveGlobalToLocal(global: WasmSinglyLinkedList<Int>): Unit =
    implementedAsIntrinsic

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun wasmShadowStackGetLocal(): Any =
    implementedAsIntrinsic

// TODO what if this is called from a lambda, and tries to read the local of the outer function somehow? Needs to read the local of the lambda instead
internal fun wasmConstructTraceFromShadowStack(): String {
    @Suppress("UNCHECKED_CAST")
    // TODO currently, this just gets the global shadow stack, which is fine, but doesn't filter out the most recent frames, which will thus always be exception initializer frames
    val shadowStackLocal = globalShadowStack
    val stackTraceAsFunctionIds = sequence {
        var cur: WasmSinglyLinkedList<Int>? = shadowStackLocal
        // iterate until we find the root frame, which has neither an elem, nor a next
        while (cur?.elem != null) {
            yield(cur.elem)
            cur = cur.next
        }
    }

    // TODO problem one: we need to construct a JsLocation somehow, which needs a bunch of info from inside the compiler (elem.getSourceLocation, JsLocation type def), not inside the stdlib. Conceptually the parameter describes this behavior, this could be an intrinsic as well,
    //      Temporary solution to problem 1: just do function names only for now

    return stackTraceAsFunctionIds.joinToString(separator = "\n  at ") {
        // TODO exact format of this string is yet TBD, so processing as well, obviously
        wasmGetStringLiteralForStringLiteralId(it)
    }
}
