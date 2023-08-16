/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.LLVMArrayType
import llvm.LLVMConstInt
import llvm.LLVMTypeRef
import org.jetbrains.kotlin.backend.common.getOrPut
import org.jetbrains.kotlin.backend.konan.llvm.ConstValue
import org.jetbrains.kotlin.backend.konan.llvm.StaticData
import org.jetbrains.kotlin.backend.konan.llvm.constValue
import org.jetbrains.kotlin.backend.konan.llvm.toLLVMType
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstantPrimitive
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.Name

// TODO: Find a better home for this function than Context.
internal fun Context.getTypeConversion(actualType: IrType, expectedType: IrType): IrSimpleFunctionSymbol? =
        getTypeConversionImpl(actualType.getInlinedClassNative(), expectedType.getInlinedClassNative())

private fun Context.getTypeConversionImpl(
        actualInlinedClass: IrClass?,
        expectedInlinedClass: IrClass?
): IrSimpleFunctionSymbol? {
    if (actualInlinedClass == expectedInlinedClass) return null

    return when {
        actualInlinedClass != null && expectedInlinedClass == null -> getBoxFunction(actualInlinedClass)
        actualInlinedClass == null && expectedInlinedClass != null -> getUnboxFunction(expectedInlinedClass)
        else -> error("actual type is ${actualInlinedClass?.fqNameForIrSerialization}, expected ${expectedInlinedClass?.fqNameForIrSerialization}")
    }.symbol
}

internal object DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION : IrDeclarationOriginImpl("INLINE_CLASS_SPECIAL_FUNCTION")

private fun IrClass.defaultOrNullableType(hasQuestionMark: Boolean) =
        if (hasQuestionMark) this.defaultType.makeNullable() else this.defaultType

internal fun Context.getBoxFunction(inlinedClass: IrClass): IrSimpleFunction = mapping.boxFunctions.getOrPut(inlinedClass) {
    require(inlinedClass.isUsedAsBoxClass())
    val classes = mutableListOf(inlinedClass)
    var parent = inlinedClass.parent
    while (parent is IrClass) {
        classes.add(parent)
        parent = parent.parent
    }
    require(parent is IrFile || parent is IrExternalPackageFragment) { "Local inline classes are not supported" }

    val isNullable = inlinedClass.inlinedClassIsNullable()
    val unboxedType = inlinedClass.defaultOrNullableType(isNullable)
    val boxedType = if (isNullable) ir.symbols.irBuiltIns.anyNType else ir.symbols.irBuiltIns.anyType

    irFactory.buildFun {
        startOffset = inlinedClass.startOffset
        endOffset = inlinedClass.endOffset
        origin = DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
        name = Name.special("<${classes.reversed().joinToString(".") { it.name.asString() }}-box>")
        returnType = boxedType
    }.also { function ->
        function.parent = parent
        function.attributeOwnerId = inlinedClass // To be able to get the file.

        function.addValueParameter {
            startOffset = inlinedClass.startOffset
            endOffset = inlinedClass.endOffset
            name = Name.identifier("value")
            type = unboxedType
        }
    }
}

internal fun Context.getUnboxFunction(inlinedClass: IrClass): IrSimpleFunction = mapping.unboxFunctions.getOrPut(inlinedClass) {
    require(inlinedClass.isUsedAsBoxClass())
    val classes = mutableListOf(inlinedClass)
    var parent = inlinedClass.parent
    while (parent is IrClass) {
        classes.add(parent)
        parent = parent.parent
    }
    require(parent is IrFile || parent is IrExternalPackageFragment) { "Local inline classes are not supported" }

    val isNullable = inlinedClass.inlinedClassIsNullable()
    val unboxedType = inlinedClass.defaultOrNullableType(isNullable)
    val boxedType = if (isNullable) ir.symbols.irBuiltIns.anyNType else ir.symbols.irBuiltIns.anyType

    irFactory.buildFun {
        startOffset = inlinedClass.startOffset
        endOffset = inlinedClass.endOffset
        origin = DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION
        name = Name.special("<${classes.reversed().joinToString(".") { it.name.asString() } }-unbox>")
        returnType = unboxedType
    }.also { function ->
        function.parent = parent
        function.attributeOwnerId = inlinedClass // To be able to get the file.

        function.addValueParameter {
            startOffset = inlinedClass.startOffset
            endOffset = inlinedClass.endOffset
            name = Name.identifier("value")
            type = boxedType
        }
    }
}

/**
 * Initialize static boxing.
 * If output target is native binary then the cache is created.
 */
internal fun initializeCachedBoxes(generationState: NativeGenerationState) {
    BoxCache.values().forEach { cache ->
        val cacheName = "${cache.name}_CACHE"
        val rangeStart = "${cache.name}_RANGE_FROM"
        val rangeEnd = "${cache.name}_RANGE_TO"
        initCache(cache, generationState, cacheName, rangeStart, rangeEnd,
                declareOnly = !generationState.shouldDefineCachedBoxes
        ).also { generationState.llvm.boxCacheGlobals[cache] = it }
    }
}

/**
 * Adds global that refers to the cache.
 */
private fun initCache(cache: BoxCache, generationState: NativeGenerationState, cacheName: String,
                      rangeStartName: String, rangeEndName: String, declareOnly: Boolean) : StaticData.Global {

    val context = generationState.context
    val kotlinType = context.irBuiltIns.getKotlinClass(cache)
    val staticData = generationState.llvm.staticData
    val llvm = generationState.llvm
    val llvmType = kotlinType.defaultType.toLLVMType(llvm)
    val llvmBoxType = llvm.structType(llvm.runtime.objHeaderType, llvmType)
    val (start, end) = context.config.target.getBoxCacheRange(cache)

    return if (declareOnly) {
        staticData.createGlobal(LLVMArrayType(llvmBoxType, end - start + 1)!!, cacheName, true)
    } else {
        // Constancy of these globals allows LLVM's constant propagation and DCE
        // to remove fast path of boxing function in case of empty range.
        staticData.placeGlobal(rangeStartName, createConstant(llvmType, start), true)
                .setConstant(true)
        staticData.placeGlobal(rangeEndName, createConstant(llvmType, end), true)
                .setConstant(true)
        val values = (start..end).map { staticData.createConstKotlinObjectBody(kotlinType, createConstant(llvmType, it)) }
        staticData.placeGlobalArray(cacheName, llvmBoxType, values, true).also {
            it.setConstant(true)
        }
    }
}

internal fun IrConstantPrimitive.toBoxCacheValue(generationState: NativeGenerationState): ConstValue? {
    val irBuiltIns = generationState.context.irBuiltIns
    val cacheType = when (value.type) {
        irBuiltIns.booleanType -> BoxCache.BOOLEAN
        irBuiltIns.byteType -> BoxCache.BYTE
        irBuiltIns.shortType -> BoxCache.SHORT
        irBuiltIns.charType -> BoxCache.CHAR
        irBuiltIns.intType -> BoxCache.INT
        irBuiltIns.longType -> BoxCache.LONG
        else -> return null
    }
    val value = when (value.kind) {
        IrConstKind.Boolean -> if (value.value as Boolean) 1L else 0L
        IrConstKind.Byte -> (value.value as Byte).toLong()
        IrConstKind.Short -> (value.value as Short).toLong()
        IrConstKind.Char -> (value.value as Char).code.toLong()
        IrConstKind.Int -> (value.value as Int).toLong()
        IrConstKind.Long -> value.value as Long
        else -> throw IllegalArgumentException("IrConst of kind ${value.kind} can't be converted to box cache")
    }
    val (start, end) = generationState.config.target.getBoxCacheRange(cacheType)
    return if (value in start..end) {
        generationState.llvm.let { llvm ->
            llvm.boxCacheGlobals[cacheType]?.pointer?.getElementPtr(llvm, value.toInt() - start)?.getElementPtr(llvm, 0)
        }
    } else {
        null
    }
}

private fun createConstant(llvmType: LLVMTypeRef, value: Int): ConstValue =
        constValue(LLVMConstInt(llvmType, value.toLong(), 1)!!)

// When start is greater than end then `inRange` check is always false
// and can be eliminated by LLVM.
private val emptyRange = 1 to 0

// Memory usage is around 20kb.
private val BoxCache.defaultRange get() = when (this) {
    BoxCache.BOOLEAN -> (0 to 1)
    BoxCache.BYTE -> (-128 to 127)
    BoxCache.SHORT -> (-128 to 127)
    BoxCache.CHAR -> (0 to 255)
    BoxCache.INT -> (-128 to 127)
    BoxCache.LONG -> (-128 to 127)
}

private fun KonanTarget.getBoxCacheRange(cache: BoxCache): Pair<Int, Int> = when (this) {
    is KonanTarget.ZEPHYR   -> emptyRange
    else                    -> cache.defaultRange
}

internal fun IrBuiltIns.getKotlinClass(cache: BoxCache): IrClass = when (cache) {
    BoxCache.BOOLEAN -> booleanClass
    BoxCache.BYTE -> byteClass
    BoxCache.SHORT -> shortClass
    BoxCache.CHAR -> charClass
    BoxCache.INT -> intClass
    BoxCache.LONG -> longClass
}.owner

// TODO: consider adding box caches for unsigned types.
enum class BoxCache {
    BOOLEAN, BYTE, SHORT, CHAR, INT, LONG
}
