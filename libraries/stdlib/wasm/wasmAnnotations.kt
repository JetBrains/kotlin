/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.annotation.AnnotationTarget.*

// Exclude declaration or file from lowerings and code generation
@Target(FILE, CLASS, FUNCTION, PROPERTY)
@Retention(AnnotationRetention.BINARY)
internal annotation class ExcludedFromCodegen

@ExcludedFromCodegen
@Target(FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
internal annotation class WasmImport(val module: String, val name: String)

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
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@ExcludedFromCodegen
internal annotation class WasmInstruction(val mnemonic: String) {
    companion object {
        const val NOP = "nop"
        const val UNREACHABLE = "unreachable"
        const val I32_EQZ = "i32.eqz"
        const val I32_EQ = "i32.eq"
        const val I32_NE = "i32.ne"
        const val I32_LT_S = "i32.lt_s"
        const val I32_LT_U = "i32.lt_u"
        const val I32_GT_S = "i32.gt_s"
        const val I32_GT_U = "i32.gt_u"
        const val I32_LE_S = "i32.le_s"
        const val I32_LE_U = "i32.le_u"
        const val I32_GE_S = "i32.ge_s"
        const val I32_GE_U = "i32.ge_u"
        const val I64_EQZ = "i64.eqz"
        const val I64_EQ = "i64.eq"
        const val I64_NE = "i64.ne"
        const val I64_LT_S = "i64.lt_s"
        const val I64_LT_U = "i64.lt_u"
        const val I64_GT_S = "i64.gt_s"
        const val I64_GT_U = "i64.gt_u"
        const val I64_LE_S = "i64.le_s"
        const val I64_LE_U = "i64.le_u"
        const val I64_GE_S = "i64.ge_s"
        const val I64_GE_U = "i64.ge_u"
        const val F32_EQ = "f32.eq"
        const val F32_NE = "f32.ne"
        const val F32_LT = "f32.lt"
        const val F32_GT = "f32.gt"
        const val F32_LE = "f32.le"
        const val F32_GE = "f32.ge"
        const val F64_EQ = "f64.eq"
        const val F64_NE = "f64.ne"
        const val F64_LT = "f64.lt"
        const val F64_GT = "f64.gt"
        const val F64_LE = "f64.le"
        const val F64_GE = "f64.ge"
        const val I32_CLZ = "i32.clz"
        const val I32_CTZ = "i32.ctz"
        const val I32_POPCNT = "i32.popcnt"
        const val I32_ADD = "i32.add"
        const val I32_SUB = "i32.sub"
        const val I32_MUL = "i32.mul"
        const val I32_DIV_S = "i32.div_s"
        const val I32_DIV_U = "i32.div_u"
        const val I32_REM_S = "i32.rem_s"
        const val I32_REM_U = "i32.rem_u"
        const val I32_AND = "i32.and"
        const val I32_OR = "i32.or"
        const val I32_XOR = "i32.xor"
        const val I32_SHL = "i32.shl"
        const val I32_SHR_S = "i32.shr_s"
        const val I32_SHR_U = "i32.shr_u"
        const val I32_ROTL = "i32.rotl"
        const val I32_ROTR = "i32.rotr"
        const val I64_CLZ = "i64.clz"
        const val I64_CTZ = "i64.ctz"
        const val I64_POPCNT = "i64.popcnt"
        const val I64_ADD = "i64.add"
        const val I64_SUB = "i64.sub"
        const val I64_MUL = "i64.mul"
        const val I64_DIV_S = "i64.div_s"
        const val I64_DIV_U = "i64.div_u"
        const val I64_REM_S = "i64.rem_s"
        const val I64_REM_U = "i64.rem_u"
        const val I64_AND = "i64.and"
        const val I64_OR = "i64.or"
        const val I64_XOR = "i64.xor"
        const val I64_SHL = "i64.shl"
        const val I64_SHR_S = "i64.shr_s"
        const val I64_SHR_U = "i64.shr_u"
        const val I64_ROTL = "i64.rotl"
        const val I64_ROTR = "i64.rotr"
        const val F32_ABS = "f32.abs"
        const val F32_NEG = "f32.neg"
        const val F32_CEIL = "f32.ceil"
        const val F32_FLOOR = "f32.floor"
        const val F32_TRUNC = "f32.trunc"
        const val F32_NEAREST = "f32.nearest"
        const val F32_SQRT = "f32.sqrt"
        const val F32_ADD = "f32.add"
        const val F32_SUB = "f32.sub"
        const val F32_MUL = "f32.mul"
        const val F32_DIV = "f32.div"
        const val F32_FMIN = "f32.fmin"
        const val F32_FMAX = "f32.fmax"
        const val F32_COPYSIGN = "f32.copysign"
        const val F64_ABS = "f64.abs"
        const val F64_NEG = "f64.neg"
        const val F64_CEIL = "f64.ceil"
        const val F64_FLOOR = "f64.floor"
        const val F64_TRUNC = "f64.trunc"
        const val F64_NEAREST = "f64.nearest"
        const val F64_SQRT = "f64.sqrt"
        const val F64_ADD = "f64.add"
        const val F64_SUB = "f64.sub"
        const val F64_MUL = "f64.mul"
        const val F64_DIV = "f64.div"
        const val F64_FMIN = "f64.fmin"
        const val F64_FMAX = "f64.fmax"
        const val F64_COPYSIGN = "f64.copysign"
        const val I32_WRAP_I64 = "i32.wrap/i64"
        const val I32_TRUNC_F32_S = "i32.trunc_s/f32"
        const val I32_TRUNC_F32_U = "i32.trunc_u/f32"
        const val I32_TRUNC_F64_S = "i32.trunc_s/f64"
        const val I32_TRUNC_F64_U = "i32.trunc_u/f64"
        const val I64_EXTEND_I32_S = "i64.extend_s/i32"
        const val I64_EXTEND_I32_U = "i64.extend_u/i32"
        const val I64_TRUNC_F32_S = "i64.trunc_s/f32"
        const val I64_TRUNC_F32_U = "i64.trunc_u/f32"
        const val I64_TRUNC_F64_S = "i64.trunc_s/f64"
        const val I64_TRUNC_F64_U = "i64.trunc_u/f64"
        const val F32_CONVERT_I32_S = "f32.convert_s/i32"
        const val F32_CONVERT_I32_U = "f32.convert_u/i32"
        const val F32_CONVERT_I64_S = "f32.convert_s/i64"
        const val F32_CONVERT_I64_U = "f32.convert_u/i64"
        const val F64_CONVERT_I32_S = "f64.convert_s/i32"
        const val F64_CONVERT_I32_U = "f64.convert_u/i32"
        const val F64_CONVERT_I64_S = "f64.convert_s/i64"
        const val F64_CONVERT_I64_U = "f64.convert_u/i64"
        const val F32_DEMOTE_F64 = "f32.demote/f64"
        const val F64_PROMOTE_F32 = "f64.promote/f32"
        const val I32_REINTERPRET_F32 = "i32.reinterpret/f32"
        const val I64_REINTERPRET_F64 = "i64.reinterpret/f64"
        const val F32_REINTERPRET_I32 = "f32.reinterpret/i32"

        const val F32_CONST_NAN = "f32.const nan"
        const val F64_CONST_NAN = "f64.const nan"
        const val F32_CONST_PLUS_INF = "f32.const +inf"
        const val F32_CONST_MINUS_INF = "f32.const -inf"
        const val F64_CONST_PLUS_INF = "f64.const +inf"
        const val F64_CONST_MINUS_INF = "f64.const -inf"
    }
}

@ExcludedFromCodegen
internal val implementedAsIntrinsic: Nothing
    get() = null!!