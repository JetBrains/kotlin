/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate.variables

import com.sun.jdi.*
import org.jetbrains.kotlin.fileClasses.internalNameWithoutInnerClasses
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.evaluate.variables.VariableFinder.Result
import org.jetbrains.kotlin.idea.debugger.isSubtype
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Type as AsmType
import com.sun.jdi.Type as JdiType
import kotlin.jvm.internal.Ref

@Suppress("SpellCheckingInspection")
class EvaluatorValueConverter(val context: ExecutionContext) {
    private companion object {
        private val UNBOXING_METHOD_NAMES = mapOf(
            "java/lang/Boolean" to "booleanValue",
            "java/lang/Character" to "charValue",
            "java/lang/Byte" to "byteValue",
            "java/lang/Short" to "shortValue",
            "java/lang/Integer" to "intValue",
            "java/lang/Float" to "floatValue",
            "java/lang/Long" to "longValue",
            "java/lang/Double" to "doubleValue"
        )
    }

    // Nearly accurate: doesn't do deep checks for Ref wrappers. Use `coerce()` for more precise check.
    fun typeMatches(requestedType: AsmType, actualTypeObj: JdiType?): Boolean {
        if (actualTypeObj == null) return true

        // Main path
        if (requestedType.descriptor == "Ljava/lang/Object;" || actualTypeObj.isSubtype(requestedType)) {
            return true
        }

        val actualType = actualTypeObj.asmType()

        fun isRefWrapper(wrapperType: AsmType, objType: AsmType): Boolean {
            return !objType.isPrimitiveType && wrapperType.className == Ref.ObjectRef::class.java.name
        }

        if (isRefWrapper(actualType, requestedType) || isRefWrapper(requestedType, actualType)) {
            return true
        }

        if (requestedType == AsmTypes.K_CLASS_TYPE && actualType == AsmTypes.JAVA_CLASS_TYPE) {
            // KClass can be represented as a Java class for simpler cases. See BoxingInterpreter.isJavaLangClassBoxing().
            return true
        }

        val unwrappedActualType = unwrap(actualType)
        val unwrappedRequestedType = unwrap(requestedType)
        return unwrappedActualType == unwrappedRequestedType
    }

    fun coerce(value: Value?, type: AsmType): Result? {
        val unrefResult = coerceRef(value, type) ?: return null
        return coerceBoxing(unrefResult.value, type)
    }

    private fun coerceRef(value: Value?, type: AsmType): Result? {
        when {
            type.isRefType -> {
                if (value != null && value.asmType().isRefType) {
                    return Result(value)
                }

                return Result(ref(value))
            }
            value != null && value.asmType().isRefType -> {
                if (type.isRefType) {
                    return Result(value)
                }

                return Result(unref(value))
            }
            else -> return Result(value)
        }
    }

    private fun coerceBoxing(value: Value?, type: AsmType): Result? {
        when {
            value == null -> return Result(value)
            type == AsmType.VOID_TYPE -> return Result(context.vm.mirrorOfVoid())
            type.isBoxedType -> {
                if (value.asmType().isBoxedType) {
                    return Result(value)
                }

                if (value !is PrimitiveValue) {
                    return null
                }

                return Result(box(value))
            }
            type.isPrimitiveType -> {
                if (value is PrimitiveValue) {
                    return Result(value)
                }

                if (value !is ObjectReference || !value.asmType().isBoxedType) {
                    return null
                }

                return Result(unbox(value))
            }
            value is PrimitiveValue -> {
                if (type.sort != AsmType.OBJECT) {
                    return null
                }

                val boxedValue = box(value)
                if (!typeMatches(type, boxedValue?.type())) {
                    return null
                }

                return Result(boxedValue)
            }
            else -> return Result(value)
        }
    }

    private fun box(value: Value?): Value? {
        if (value !is PrimitiveValue) {
            return value
        }

        val unboxedType = value.asmType()
        val boxedType = box(unboxedType)

        val boxedTypeClass = (context.findClass(boxedType) as ClassType?)
            ?: error("Class $boxedType is not loaded")

        val methodDesc = AsmType.getMethodDescriptor(boxedType, unboxedType)
        val valueOfMethod = boxedTypeClass.methodsByName("valueOf", methodDesc).first()

        return context.invokeMethod(boxedTypeClass, valueOfMethod, listOf(value))
    }

    private fun unbox(value: Value?): Value? {
        if (value !is ObjectReference) {
            return value
        }

        val boxedTypeClass = value.referenceType() as? ClassType ?: return value
        val boxedType = boxedTypeClass.asmType().takeIf { it.isBoxedType } ?: return value
        val unboxedType = unbox(boxedType)

        val unboxingMethodName = UNBOXING_METHOD_NAMES.getValue(boxedType.internalName)
        val methodDesc = AsmType.getMethodDescriptor(unboxedType)
        val valueMethod = boxedTypeClass.methodsByName(unboxingMethodName, methodDesc).first()
        return context.invokeMethod(value, valueMethod, emptyList())
    }

    private fun ref(value: Value?): Value? {
        if (value is VoidValue) {
            return value
        }

        fun wrapRef(value: Value?, refTypeClass: ClassType): Value? {
            val constructor = refTypeClass.methods().single { it.isConstructor }
            val ref = context.newInstance(refTypeClass, constructor, emptyList())
            context.keepReference(ref)

            val elementField = refTypeClass.fieldByName("element") ?: error("'element' field not found")
            ref.setValue(elementField, value)
            return ref
        }

        if (value is PrimitiveValue) {
            val primitiveType = value.asmType()
            val refType = PRIMITIVE_TO_REF.getValue(primitiveType)

            val refTypeClass = (context.findClass(refType) as ClassType?)
                ?: error("Class $refType is not loaded")

            return wrapRef(value, refTypeClass)
        } else {
            val refType = AsmType.getType(Ref.ObjectRef::class.java)
            val refTypeClass = (context.findClass(refType) as ClassType?)
                ?: error("Class $refType is not loaded")

            return wrapRef(value, refTypeClass)
        }
    }

    fun unref(value: Value?): Value? {
        if (value !is ObjectReference) {
            return value
        }

        val type = value.type()
        if (type !is ClassType || !type.signature().startsWith("L" + AsmTypes.REF_TYPE_PREFIX)) {
            return value
        }

        val field = type.fieldByName("element") ?: return value
        return value.getValue(field)
    }
}

private fun unbox(type: AsmType): AsmType {
    if (type.sort == AsmType.OBJECT) {
        return BOXED_TO_PRIMITIVE[type] ?: type
    }

    return type
}

private fun box(type: AsmType): AsmType {
    if (type.isPrimitiveType) {
        return PRIMITIVE_TO_BOXED[type] ?: type
    }

    return type
}

private fun unwrap(type: AsmType): AsmType {
    if (type.sort != AsmType.OBJECT) {
        return type
    }

    return REF_TO_PRIMITIVE[type] ?: BOXED_TO_PRIMITIVE[type] ?: type
}

private val AsmType.isPrimitiveType: Boolean
    get() = sort != AsmType.OBJECT && sort != AsmType.ARRAY

internal val AsmType.isRefType: Boolean
    get() = sort == AsmType.OBJECT && this in REF_TYPES

internal val Value?.isRefType: Boolean
    get() = this is ObjectReference && AsmType.getType(this.referenceType().signature()).isRefType

private val AsmType.isBoxedType: Boolean
    get() = this in BOXED_TO_PRIMITIVE

private fun Value.asmType(): AsmType {
    return type().asmType()
}

private fun JdiType.asmType(): AsmType {
    return AsmType.getType(signature())
}

private val BOXED_TO_PRIMITIVE: Map<AsmType, AsmType> = JvmPrimitiveType.values()
    .map { Pair(AsmType.getObjectType(it.wrapperFqName.internalNameWithoutInnerClasses), AsmType.getType(it.desc)) }
    .toMap()

private val PRIMITIVE_TO_BOXED: Map<AsmType, AsmType> = BOXED_TO_PRIMITIVE.map { (k, v) -> Pair(v, k) }.toMap()

private val REF_TO_PRIMITIVE = mapOf(
    Ref.ByteRef::class.java.name to AsmType.BYTE_TYPE,
    Ref.ShortRef::class.java.name to AsmType.SHORT_TYPE,
    Ref.IntRef::class.java.name to AsmType.INT_TYPE,
    Ref.LongRef::class.java.name to AsmType.LONG_TYPE,
    Ref.FloatRef::class.java.name to AsmType.FLOAT_TYPE,
    Ref.DoubleRef::class.java.name to AsmType.DOUBLE_TYPE,
    Ref.CharRef::class.java.name to AsmType.CHAR_TYPE,
    Ref.BooleanRef::class.java.name to AsmType.BOOLEAN_TYPE
).mapKeys { (k, _) -> AsmType.getObjectType(k.replace('.', '/')) }

private val PRIMITIVE_TO_REF: Map<AsmType, AsmType> = REF_TO_PRIMITIVE.map { (k, v) -> Pair(v, k) }.toMap()

private val REF_TYPES: Set<AsmType> = REF_TO_PRIMITIVE.keys + AsmType.getType(Ref.ObjectRef::class.java)