/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.v2.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicHandlerType
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols

class JvmAtomicSymbols(
    context: IrPluginContext,
    moduleFragment: IrModuleFragment
) : AbstractAtomicSymbols(context, moduleFragment) {

    private val javaLang: IrPackageFragment = createPackage("java.lang")
    private val kotlinJvm: IrPackageFragment = createPackage("kotlin.jvm")
    private val javaUtilConcurrentAtomic: IrPackageFragment = createPackage("java.util.concurrent.atomic")
    private val javaLangClass: IrClassSymbol = createClass(javaLang, "Class", ClassKind.CLASS, Modality.FINAL)

    override val volatileAnnotationClass: IrClass
        get() = context.finderForBuiltins().findClass(ClassId(FqName("kotlin.jvm"), Name.identifier("Volatile")))?.owner
            ?: error("kotlin.jvm.Volatile class is not found")

    // java.util.concurrent.atomic.AtomicIntegerFieldUpdater
    val javaAtomicIntegerFieldUpdaterClass: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicIntegerFieldUpdater", irBuiltIns.intType, AtomicHandlerType.ATOMIC_FIELD_UPDATER)
    }

    // java.util.concurrent.atomic.AtomicLongFieldUpdater
    val javaAtomicLongFieldUpdaterClass: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicLongFieldUpdater", irBuiltIns.longType, AtomicHandlerType.ATOMIC_FIELD_UPDATER)
    }

    // java.util.concurrent.atomic.AtomicReferenceFieldUpdater
    val javaAtomicRefFieldUpdaterClass: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicReferenceFieldUpdater", irBuiltIns.anyType, AtomicHandlerType.ATOMIC_FIELD_UPDATER)
    }

    // java.util.concurrent.atomic.AtomicIntegerArray
    override val atomicIntArrayClassSymbol: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicIntegerArray", irBuiltIns.intType, AtomicHandlerType.ATOMIC_ARRAY)
    }

    // java.util.concurrent.atomic.AtomicLongArray
    override val atomicLongArrayClassSymbol: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicLongArray", irBuiltIns.longType, AtomicHandlerType.ATOMIC_ARRAY)
    }

    // java.util.concurrent.atomic.AtomicReferenceArray
    override val atomicRefArrayClassSymbol: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicReferenceArray", irBuiltIns.anyType, AtomicHandlerType.ATOMIC_ARRAY)
    }

    // java.util.concurrent.atomic.AtomicInteger
    val javaAtomicIntegerClass: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicInteger", irBuiltIns.intType, AtomicHandlerType.BOXED_ATOMIC)
    }

    // java.util.concurrent.atomic.AtomicLong
    val javaAtomicLongClass: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicLong", irBuiltIns.longType, AtomicHandlerType.BOXED_ATOMIC)
    }

    // java.util.concurrent.atomic.AtomicReference
    val javaAtomicReferenceClass: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicReference", irBuiltIns.anyType, AtomicHandlerType.BOXED_ATOMIC)
    }

    // java.util.concurrent.atomic.AtomicBoolean
    val javaAtomicBooleanClass: IrClassSymbol by lazy {
        createJavaAtomicClass("AtomicBoolean", irBuiltIns.booleanType, AtomicHandlerType.BOXED_ATOMIC)
    }

    fun newUpdater(atomicUpdaterClassSymbol: IrClassSymbol): IrSimpleFunctionSymbol =
        atomicUpdaterClassSymbol.getSimpleFunction("newUpdater")
            ?: error("No newUpdater function was found for ${atomicUpdaterClassSymbol.owner.render()} ")

    private val BOXED_ATOMIC_TYPES = setOf(
        javaAtomicIntegerClass,
        javaAtomicLongClass,
        javaAtomicBooleanClass,
        javaAtomicReferenceClass
    )

    private val ATOMIC_FIELD_UPDATER_TYPES = setOf(
        javaAtomicIntegerFieldUpdaterClass,
        javaAtomicLongFieldUpdaterClass,
        javaAtomicRefFieldUpdaterClass
    )

    fun isAtomicFieldUpdaterHandlerType(type: IrType) = type.classOrNull in ATOMIC_FIELD_UPDATER_TYPES
    fun isBoxedAtomicHandlerType(type: IrType) = type.classOrNull in BOXED_ATOMIC_TYPES

    fun javaFUClassSymbol(valueType: IrType): IrClassSymbol =
        when {
            valueType.isInt() -> javaAtomicIntegerFieldUpdaterClass
            valueType.isLong() -> javaAtomicLongFieldUpdaterClass
            valueType.isBoolean() -> javaAtomicIntegerFieldUpdaterClass
            !valueType.isPrimitiveType() -> javaAtomicRefFieldUpdaterClass
            else -> error("Non of the Java field updater types Atomic(Integer|Long|Reference)FieldUpdater can be used to atomically update a property of the given type: ${valueType.render()}")
        }

    fun javaAtomicBoxClassSymbol(valueType: IrType): IrClassSymbol =
        when {
            valueType.isInt() -> javaAtomicIntegerClass
            valueType.isLong() -> javaAtomicLongClass
            valueType.isBoolean() -> javaAtomicBooleanClass
            !valueType.isPrimitiveType() -> javaAtomicReferenceClass
            else -> error("Non of the boxed Java atomic types Atomic(Integer|Long|Boolean|Reference) can be used to atomically update a property of the given type: ${valueType.render()}")
        }

    private fun createJavaAtomicClass(
        shortClassName: String,
        valueType: IrType,
        atomicHandlerType: AtomicHandlerType
    ): IrClassSymbol {
        fun IrFunctionBuilder.addReferenceTypeParameter(valueType: IrType): IrType =
            if (valueType.isAny()) {
                typeParameter("T", irBuiltIns.anyNType).defaultType
            } else {
                valueType
            }

        fun IrFunctionBuilder.addIndexForArrayType() {
            if (atomicHandlerType == AtomicHandlerType.ATOMIC_ARRAY) valueParameter("i", irBuiltIns.intType)
        }

        fun IrFunctionBuilder.addObjForFieldUpdaterClass() {
            if (atomicHandlerType == AtomicHandlerType.ATOMIC_FIELD_UPDATER) valueParameter("obj", irBuiltIns.anyType)
        }
        return createClass(javaUtilConcurrentAtomic, shortClassName, ClassKind.CLASS, Modality.FINAL).apply {
            val classType = defaultType
            // Add constructor
            when (atomicHandlerType) {
                AtomicHandlerType.ATOMIC_FIELD_UPDATER -> {
                    owner.irStaticFunction {
                        name = Name.identifier("newUpdater")
                        returnType = classType
                        valueParameter("tclass", javaLangClass.starProjectedType)
                        if (valueType.isAny()) valueParameter("vclass", javaLangClass.starProjectedType)
                        valueParameter("fieldName", irBuiltIns.stringType)
                    }
                }
                AtomicHandlerType.ATOMIC_ARRAY -> {
                    owner.irConstructor {
                        addReferenceTypeParameter(valueType)
                        valueParameter("length", irBuiltIns.intType)
                    }
                    if (valueType.isAny()) {
                        owner.irConstructor {
                            addReferenceTypeParameter(valueType)
                            valueParameter("array", irBuiltIns.arrayClass.defaultType)
                        }
                    }
                }
                AtomicHandlerType.BOXED_ATOMIC -> {
                    owner.irConstructor {
                        valueParameter("value", valueType)
                    }
                }
                else -> {}
            }
            // Declare functions
            owner.irFunction {
                name = Name.identifier("get")
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                returnType = addReferenceTypeParameter(valueType)
            }
            owner.irFunction {
                name = Name.identifier("set")
                returnType = irBuiltIns.unitType
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                val T = addReferenceTypeParameter(valueType)
                valueParameter("newValue", T)
            }
            owner.irFunction {
                name = Name.identifier("compareAndSet")
                returnType = irBuiltIns.booleanType
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                val T = addReferenceTypeParameter(valueType)
                valueParameter("expect", T)
                valueParameter("update", T)
            }
            owner.irFunction {
                name = Name.identifier("lazySet")
                returnType = irBuiltIns.unitType
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                val T = addReferenceTypeParameter(valueType)
                valueParameter("newValue", T)
            }
            owner.irFunction {
                name = Name.identifier("getAndSet")
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                val T = addReferenceTypeParameter(valueType)
                valueParameter("newValue", T)
                returnType = T
            }
            if (valueType.isInt() || valueType.isLong()) {
                owner.irFunction {
                    name = Name.identifier("addAndGet")
                    returnType = valueType
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                    valueParameter("delta", valueType)
                }
                owner.irFunction {
                    name = Name.identifier("getAndAdd")
                    returnType = valueType
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                    valueParameter("delta", valueType)
                }
                owner.irFunction {
                    name = Name.identifier("incrementAndGet")
                    returnType = valueType
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                }
                owner.irFunction {
                    name = Name.identifier("getAndIncrement")
                    returnType = valueType
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                }
                owner.irFunction {
                    name = Name.identifier("decrementAndGet")
                    returnType = valueType
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                }
                owner.irFunction {
                    name = Name.identifier("getAndDecrement")
                    returnType = valueType
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                }
            }
        }
    }

    private fun createClass(
        irPackage: IrPackageFragment,
        shortName: String,
        classKind: ClassKind,
        classModality: Modality,
        isValueClass: Boolean = false,
    ): IrClassSymbol = irFactory.irClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = classModality
        isValue = isValueClass
        parent = irPackage
        thisReceiver()
    }.symbol

    private val kotlinKClassJava: IrPropertySymbol = irFactory.irProperty {
        name = Name.identifier("java")
        parent = kotlinJvm
        getter {
            extensionReceiver {
                name = "receiver".synthesizedName
                type = irBuiltIns.kClassClass.starProjectedType
            }
            returnType = javaLangClass.defaultType
        }
    }.symbol

    private fun kClassToJavaClass(kClassReference: IrExpression): IrCall =
        buildIrGet(javaLangClass.starProjectedType, null, kotlinKClassJava.owner.getter!!.symbol).apply {
            arguments[0] = kClassReference
        }

    fun javaClassReference(classType: IrType): IrCall = kClassToJavaClass(kClassReference(classType))

    private fun kClassReference(classType: IrType): IrClassReferenceImpl =
        IrClassReferenceImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.kClassClass.starProjectedType, irBuiltIns.kClassClass, classType
        )

    private fun buildIrGet(
        type: IrType,
        receiver: IrExpression?,
        getterSymbol: IrFunctionSymbol
    ): IrCall = IrCallImpl(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
        type,
        getterSymbol as IrSimpleFunctionSymbol,
        typeArgumentsCount = getterSymbol.owner.typeParameters.size,
        origin = IrStatementOrigin.GET_PROPERTY
    ).apply {
        dispatchReceiver = receiver
    }

    override fun createBuilder(symbol: IrSymbol): JvmAtomicfuIrBuilder =
        JvmAtomicfuIrBuilder(this, symbol)
}
