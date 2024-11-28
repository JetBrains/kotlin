/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createExtensionReceiver
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
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
        get() = context.referenceClass(ClassId(FqName("kotlin.jvm"), Name.identifier("Volatile")))?.owner
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
        fun IrFunction.addReferenceTypeParameter(valueType: IrType): IrType =
            if (valueType.isAny()) {
                addTypeParameter("T", irBuiltIns.anyNType).defaultType
            } else {
                valueType
            }

        fun IrFunction.addIndexForArrayType() {
            if (atomicHandlerType == AtomicHandlerType.ATOMIC_ARRAY) addValueParameter("i", irBuiltIns.intType)
        }

        fun IrFunction.addObjForFieldUpdaterClass() {
            if (atomicHandlerType == AtomicHandlerType.ATOMIC_FIELD_UPDATER) addValueParameter("obj", irBuiltIns.anyType)
        }
        return createClass(javaUtilConcurrentAtomic, shortClassName, ClassKind.CLASS, Modality.FINAL).apply {
            // Add constructor
            when (atomicHandlerType) {
                AtomicHandlerType.ATOMIC_FIELD_UPDATER -> {
                    owner.addFunction("newUpdater", this.defaultType, isStatic = true).apply {
                        addValueParameter("tclass", javaLangClass.starProjectedType)
                        if (valueType.isAny()) addValueParameter("vclass", javaLangClass.starProjectedType)
                        addValueParameter("fieldName", irBuiltIns.stringType)
                    }
                }
                AtomicHandlerType.ATOMIC_ARRAY -> {
                    owner.addConstructor().apply {
                        addReferenceTypeParameter(valueType)
                        addValueParameter("length", irBuiltIns.intType)
                    }
                    if (valueType.isAny()) {
                        owner.addConstructor().apply {
                            addReferenceTypeParameter(valueType)
                            addValueParameter("array", irBuiltIns.arrayClass.defaultType)
                        }
                    }
                }
                AtomicHandlerType.BOXED_ATOMIC -> {
                    owner.addConstructor().apply {
                        addValueParameter("value", valueType)
                    }
                }
                else -> {}
            }
            // Declare functions
            owner.addFunction(name = "get", returnType = valueType).apply {
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                val T = addReferenceTypeParameter(valueType)
                returnType = T
            }
            owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                val T = addReferenceTypeParameter(valueType)
                addValueParameter("newValue", T)
            }
            owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                val T = addReferenceTypeParameter(valueType)
                addValueParameter("expect", T)
                addValueParameter("update", T)
            }
            owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                val T = addReferenceTypeParameter(valueType)
                addValueParameter("newValue", T)
            }
            owner.addFunction(name = "getAndSet", returnType = valueType).apply {
                addObjForFieldUpdaterClass()
                addIndexForArrayType()
                val T = addReferenceTypeParameter(valueType)
                addValueParameter("newValue", T)
                returnType = T
            }
            if (valueType.isInt() || valueType.isLong()) {
                owner.addFunction(name = "addAndGet", returnType = valueType).apply {
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                    addValueParameter("delta", valueType)
                }
                owner.addFunction(name = "getAndAdd", returnType = valueType).apply {
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                    addValueParameter("delta", valueType)
                }
                owner.addFunction(name = "incrementAndGet", returnType = valueType).apply {
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                }
                owner.addFunction(name = "getAndIncrement", returnType = valueType).apply {
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                }
                owner.addFunction(name = "decrementAndGet", returnType = valueType).apply {
                    addObjForFieldUpdaterClass()
                    addIndexForArrayType()
                }
                owner.addFunction(name = "getAndDecrement", returnType = valueType).apply {
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
    ): IrClassSymbol = irFactory.buildClass {
        name = Name.identifier(shortName)
        kind = classKind
        modality = classModality
        isValue = isValueClass
    }.apply {
        parent = irPackage
        createThisReceiverParameter()
    }.symbol

    private val kotlinKClassJava: IrPropertySymbol = irFactory.buildProperty {
        name = Name.identifier("java")
    }.apply {
        parent = kotlinJvm
        addGetter().apply {
            parameters += createExtensionReceiver(irBuiltIns.kClassClass.starProjectedType)
            returnType = javaLangClass.defaultType
        }
    }.symbol

    private fun kClassToJavaClass(kClassReference: IrExpression): IrCall =
        buildIrGet(javaLangClass.starProjectedType, null, kotlinKClassJava.owner.getter!!.symbol).apply {
            extensionReceiver = kClassReference
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