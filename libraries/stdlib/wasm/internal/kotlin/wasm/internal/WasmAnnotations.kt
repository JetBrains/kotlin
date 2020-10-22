/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.annotation.AnnotationTarget.*

// Exclude declaration or file from lowering and code generation
@Target(FILE, CLASS, FUNCTION, CONSTRUCTOR, PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class ExcludedFromCodegen

@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmImport(val module: String, val name: String)

@Target(CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmForeign

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmReinterpret

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmPrimitive

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