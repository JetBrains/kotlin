/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import hair.compilation.FunctionCompilation
import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.common.lower.coroutines.getOrCreateFunctionWithContinuationStub
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterCodegen
import org.jetbrains.kotlin.backend.konan.cexport.CAdapterExportedElements
import org.jetbrains.kotlin.backend.konan.cgen.CBridgeOrigin
import org.jetbrains.kotlin.backend.konan.hair.HairToBitcode
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.processBindClassToObjCNameAnnotations
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ReifiedFunctionLowering.Companion.isReifiedInline
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.nativeBinaryOptions.AndroidProgramType
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.config.nativeBinaryOptions.SourceInfoType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

internal fun CodeGeneratorVisitor.evaluateStringConst(value: String) =
        codegen.staticData.kotlinStringLiteral(value)

/**
 * Normalizing nans to single value is useful for build reproducibility.
 *
 * It's possible that it can lead to some bad consequences for interop libraries,
 * for which exact nan value is important. We are not aware of the existence of
 * any such useful library, at least on priority targets.
 *
 * On the other side, the semantics of exact cases, NaN values should be not normalized, is unclear.
 * E.g., in previous implementation, storing constant to another constant could change the exact bit pattern.
 *
 * So for now, we would just normalize all NaN constants. At least this leads to predictable result
 * useful in almost all cases.
 *
 * Also, java.lang classes are used here to avoid unexpected NaN values if a compiler and stdlib
 * are built in an arm64 architecture environment.
 */
internal fun Float.normalizeNan() = if (isNaN()) java.lang.Float.NaN else this
internal fun Double.normalizeNan() = if (isNaN()) java.lang.Double.NaN else this

internal fun CodeGeneratorVisitor.evaluateConst(value: IrConst): ConstValue {
    context.log{"evaluateConst                  : ${ir2string(value)}"}
    return when (value.kind) {
        IrConstKind.Null -> llvm.nullPointer
        IrConstKind.Boolean -> llvm.constInt1(value.value as Boolean)
        IrConstKind.Char -> llvm.constChar16(value.value as Char)
        IrConstKind.Byte -> llvm.constInt8(value.value as Byte)
        IrConstKind.Short -> llvm.constInt16(value.value as Short)
        IrConstKind.Int -> llvm.constInt32(value.value as Int)
        IrConstKind.Long -> llvm.constInt64(value.value as Long)
        IrConstKind.String -> evaluateStringConst(value.value as String)
        IrConstKind.Float -> llvm.constFloat32((value.value as Float).normalizeNan())
        IrConstKind.Double -> llvm.constFloat64((value.value as Double).normalizeNan())
    }
}

//-------------------------------------------------------------------------//

internal class IrConstValueCacheKey(val value: IrConstantValue) {
    override fun equals(other: Any?): Boolean {
        if (other !is IrConstValueCacheKey) return false
        return value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

internal fun CodeGeneratorVisitor.evaluateConstantValue(value: IrConstantValue): ConstValue =
        constantValuesCache.getOrPut(IrConstValueCacheKey(value)) {
            evaluateConstantValueImpl(value)
        }

internal fun CodeGeneratorVisitor.evaluateConstantValueImpl(value: IrConstantValue): ConstValue {
    val symbols = context.symbols
    return when (value) {
        is IrConstantPrimitive -> {
            val constructedType = value.value.type
            if (context.getTypeConversion(constructedType, value.type) != null) {
                if (value.value.kind == IrConstKind.Null) {
                    Zero(value.type.toLLVMType(llvm))
                } else {
                    require(value.type.toLLVMType(llvm) == llvm.pointerType) {
                        "Can't wrap ${value.value.kind.asString} constant to type ${value.type.render()}"
                    }
                    value.toBoxCacheValue(generationState) ?: codegen.staticData.createConstKotlinObject(
                            constructedType.getClass()!!,
                            evaluateConst(value.value)
                    )
                }
            } else {
                evaluateConst(value.value)
            }
        }
        is IrConstantArray -> {
            val clazz = value.type.getClass()!!
            require(clazz.symbol == context.irBuiltIns.arrayClass || clazz.symbol in context.irBuiltIns.primitiveTypesToPrimitiveArrays.values) {
                "Statically initialized array should have array type"
            }
            codegen.staticData.createConstKotlinArray(
                    value.type.getClass()!!,
                    value.elements.map { evaluateConstantValue(it) }
            )
        }
        is IrConstantObject -> {
            val constructedType = value.constructor.owner.constructedClassType
            val constructedClass = constructedType.getClass()!!
            val needUnBoxing = constructedType.getInlinedClassNative() != null &&
                    context.getTypeConversion(constructedType, value.type) == null
            if (needUnBoxing) {
                val unboxed = value.valueArguments.singleOrNull()
                        ?: error("Inlined class should have exactly one constructor argument")
                return evaluateConstantValue(unboxed)
            }
            val fields = if (value.constructor.owner.isConstantConstructorIntrinsic) {
                intrinsicGenerator.evaluateConstantConstructorFields(value, value.valueArguments.map { evaluateConstantValue(it) })
            } else {
                val fields = context.getLayoutBuilder(constructedClass).getFields(llvm)
                val constructor = value.constructor.owner
                val parameters = constructor.parameters.associateBy { it.name.toString() }
                // support of initilaization of object in following case:
                // open class Base(val field: ...)
                // Child(val otherField: ...) : Base(constantValue)
                //
                //  Child(constantValue) could be initialized constantly. This is required for function references.
                val delegatedCallConstants = constructor.loweredConstructorFunction?.body?.statements
                        ?.filterIsInstance<IrCall>()
                        ?.singleOrNull { it.origin == LOWERED_DELEGATING_CONSTRUCTOR_CALL }
                        ?.getArgumentsWithIr()
                        ?.filter { it.second is IrConstantValue }
                        ?.associate { it.first.name.toString() to it.second }
                        .orEmpty()
                fields.map { field ->
                    val init = if (field.isConst) {
                        field.irField!!.initializer?.expression.also {
                            require(field.name !in parameters) {
                                "Constant field ${field.name} of class ${constructedClass.name} shouldn't be a constructor parameter"
                            }
                        }
                    } else {
                        val index = parameters[field.name]?.indexInParameters
                        if (index != null)
                            value.valueArguments[index]
                        else
                            delegatedCallConstants[field.name]
                    }
                    when (init) {
                        is IrConst -> evaluateConst(init)
                        is IrConstantValue -> evaluateConstantValue(init)
                        null -> error("Bad statically initialized object: field ${field.name} value not set in ${constructedClass.name}")
                        else -> error("Unexpected constant initializer type: ${init::class}")
                    }
                }.also {
                    require(it.size == value.valueArguments.size + fields.count { it.isConst } + delegatedCallConstants.size) {
                        "Bad statically initialized object of class ${constructedClass.name}: not all arguments are used"
                    }
                }
            }

            require(value.type.toLLVMType(llvm) == llvm.pointerType) { "Constant object is not an object, but ${value.type.render()}" }
            codegen.staticData.createConstKotlinObject(
                    constructedClass,
                    *fields.toTypedArray()
            )
        }
    }
}
