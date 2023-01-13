/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KClass

// Exclude declaration or file from lowering and code generation
@Target(FILE, CLASS, FUNCTION, CONSTRUCTOR, PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class ExcludedFromCodegen

@Target(CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmArrayOf(
    val type: KClass<*>,
    val isNullable: Boolean,
)

// When applied to a function it forces codegen to not generate any code for it.
// In other words the annotated function will pass it's arguments unchanged.
// This is used in order to implement type casts when we know that underlying wasm types don't change.
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmNoOpCast

// This tells backend to insert box/unbox intrinsics around the annotated class. It's used to represent built-in types without making them
// explicitly "inline" (or "value" in the newest terminology).
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmAutoboxed

/**
 *  Replace calls to this functions with specified Wasm instruction.
 *
 *  Operands are passed in the following order:
 *    1. Dispatch receiver (if present)
 *    2. Extension receiver (if present)
 *    3. Value arguments
 *
 *  @mnemonic parameter is an instruction WAT name: "i32.add", "f64.trunc", etc.
 *
 *  Immediate arguments (label, index, offest, align, etc.) are not supported yet.
 */

@ExcludedFromCodegen
internal val implementedAsIntrinsic: Nothing
    get() = null!!

/**
 * Indicates that annotated constructor is primitive
 * i.e. has direct layout from it's parameters to object fields (except any's fields) and has no code
 * In this case no need to call it during an object creation.
 */
@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmPrimitiveConstructor