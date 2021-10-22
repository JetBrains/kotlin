/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import llvm.*
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrConstantPrimitive
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultOrNullableType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.Name

internal fun KonanSymbols.getTypeConversion(actualType: IrType, expectedType: IrType): IrSimpleFunctionSymbol? =
        getTypeConversionImpl(actualType.getInlinedClassNative(), expectedType.getInlinedClassNative())

private fun KonanSymbols.getTypeConversionImpl(
        actualInlinedClass: IrClass?,
        expectedInlinedClass: IrClass?
): IrSimpleFunctionSymbol? {
    if (actualInlinedClass == expectedInlinedClass) return null

    return when {
        actualInlinedClass == null && expectedInlinedClass == null -> null
        actualInlinedClass != null && expectedInlinedClass == null -> context.getBoxFunction(actualInlinedClass)
        actualInlinedClass == null && expectedInlinedClass != null -> context.getUnboxFunction(expectedInlinedClass)
        else -> error("actual type is ${actualInlinedClass?.fqNameForIrSerialization}, expected ${expectedInlinedClass?.fqNameForIrSerialization}")
    }?.symbol
}

internal object DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION : IrDeclarationOriginImpl("INLINE_CLASS_SPECIAL_FUNCTION")

internal val Context.getBoxFunction: (IrClass) -> IrSimpleFunction by Context.lazyMapMember { inlinedClass ->
    require(inlinedClass.isUsedAsBoxClass())
    val classes = mutableListOf<IrClass>(inlinedClass)
    var parent = inlinedClass.parent
    while (parent is IrClass) {
        classes.add(parent)
        parent = parent.parent
    }
    require(parent is IrFile || parent is IrExternalPackageFragment) { "Local inline classes are not supported" }

    val symbols = ir.symbols

    val isNullable = inlinedClass.inlinedClassIsNullable()
    val unboxedType = inlinedClass.defaultOrNullableType(isNullable)
    val boxedType = symbols.any.owner.defaultOrNullableType(isNullable)

    val parameterType = unboxedType
    val returnType = boxedType

    val startOffset = inlinedClass.startOffset
    val endOffset = inlinedClass.endOffset

    IrFunctionImpl(
            startOffset, endOffset,
            DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION,
            IrSimpleFunctionSymbolImpl(),
            Name.special("<${classes.reversed().joinToString(".") { it.name.asString() }}-box>"),
            DescriptorVisibilities.PUBLIC,
            Modality.FINAL,
            returnType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false,
            isExpect = false,
            isFakeOverride = false,
            isOperator = false,
            isInfix = false
    ).also { function ->
        function.valueParameters = listOf(
            IrValueParameterImpl(
                    startOffset, endOffset,
                    IrDeclarationOrigin.DEFINED,
                    IrValueParameterSymbolImpl(),
                    Name.identifier("value"),
                    index = 0,
                    varargElementType = null,
                    isCrossinline = false,
                    type = parameterType,
                    isNoinline = false,
                    isHidden = false,
                    isAssignable = false
            ).also {
                it.parent = function
            })
        function.parent = parent
    }
}

internal val Context.getUnboxFunction: (IrClass) -> IrSimpleFunction by Context.lazyMapMember { inlinedClass ->
    require(inlinedClass.isUsedAsBoxClass())
    val classes = mutableListOf<IrClass>(inlinedClass)
    var parent = inlinedClass.parent
    while (parent is IrClass) {
        classes.add(parent)
        parent = parent.parent
    }
    require(parent is IrFile || parent is IrExternalPackageFragment) { "Local inline classes are not supported" }

    val symbols = ir.symbols

    val isNullable = inlinedClass.inlinedClassIsNullable()
    val unboxedType = inlinedClass.defaultOrNullableType(isNullable)
    val boxedType = symbols.any.owner.defaultOrNullableType(isNullable)

    val parameterType = boxedType
    val returnType = unboxedType

    val startOffset = inlinedClass.startOffset
    val endOffset = inlinedClass.endOffset

    IrFunctionImpl(
            startOffset, endOffset,
            DECLARATION_ORIGIN_INLINE_CLASS_SPECIAL_FUNCTION,
            IrSimpleFunctionSymbolImpl(),
            Name.special("<${classes.reversed().joinToString(".") { it.name.asString() } }-unbox>"),
            DescriptorVisibilities.PUBLIC,
            Modality.FINAL,
            returnType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false,
            isExpect = false,
            isFakeOverride = false,
            isOperator = false,
            isInfix = false
    ).also { function ->
        function.valueParameters = listOf(
            IrValueParameterImpl(
                    startOffset, endOffset,
                    IrDeclarationOrigin.DEFINED,
                    IrValueParameterSymbolImpl(),
                    Name.identifier("value"),
                    index = 0,
                    varargElementType = null,
                    isCrossinline = false,
                    type = parameterType,
                    isNoinline = false,
                    isHidden = false,
                    isAssignable = false
            ).also {
                it.parent = function
            })
        function.parent = parent
    }
}

/**
 * Initialize static boxing.
 * If output target is native binary then the cache is created.
 */
internal fun initializeCachedBoxes(context: Context) {
    BoxCache.values().forEach { cache ->
        val cacheName = "${cache.name}_CACHE"
        val rangeStart = "${cache.name}_RANGE_FROM"
        val rangeEnd = "${cache.name}_RANGE_TO"
        initCache(cache, context, cacheName, rangeStart, rangeEnd,
                declareOnly = !context.producedLlvmModuleContainsStdlib
        ).also { context.llvm.boxCacheGlobals[cache] = it }
    }
}

/**
 * Adds global that refers to the cache.
 */
private fun initCache(cache: BoxCache, context: Context, cacheName: String,
                      rangeStartName: String, rangeEndName: String, declareOnly: Boolean) : StaticData.Global {

    val kotlinType = context.irBuiltIns.getKotlinClass(cache)
    val staticData = context.llvm.staticData
    val llvmType = staticData.getLLVMType(kotlinType.defaultType)
    val llvmBoxType = structType(context.llvm.runtime.objHeaderType, llvmType)
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
        val values = (start..end).map { staticData.createInitializer(kotlinType, createConstant(llvmType, it)) }
        staticData.placeGlobalArray(cacheName, llvmBoxType, values, true).also {
            it.setConstant(true)
        }
    }
}

internal fun IrConstantPrimitive.toBoxCacheValue(context: Context): ConstValue? {
    val cacheType = when (value.type) {
        context.irBuiltIns.booleanType -> BoxCache.BOOLEAN
        context.irBuiltIns.byteType -> BoxCache.BYTE
        context.irBuiltIns.shortType -> BoxCache.SHORT
        context.irBuiltIns.charType -> BoxCache.CHAR
        context.irBuiltIns.intType -> BoxCache.INT
        context.irBuiltIns.longType -> BoxCache.LONG
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
    val (start, end) = context.config.target.getBoxCacheRange(cacheType)
    return if (value in start..end) {
        context.llvm.boxCacheGlobals[cacheType]?.pointer?.getElementPtr(value.toInt() - start)?.getElementPtr(0)
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
