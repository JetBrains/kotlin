/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.ATOMIC_ARRAY
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.ATOMIC_BOOLEAN
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.ATOMIC_BOOLEAN_ARRAY
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.ATOMIC_INT
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.ATOMIC_INT_ARRAY
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.ATOMIC_LONG
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.ATOMIC_LONG_ARRAY
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.ATOMIC_REF
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols

class JvmAtomicSymbols(
    context: IrPluginContext,
    moduleFragment: IrModuleFragment
): AbstractAtomicSymbols(context, moduleFragment) {
    private val javaLang: IrPackageFragment = createPackage("java.lang")
    private val javaUtilConcurrent: IrPackageFragment = createPackage("java.util.concurrent.atomic")
    private val kotlinJvm: IrPackageFragment = createPackage("kotlin.jvm")
    private val javaLangClass: IrClassSymbol = createClass(javaLang, "Class", ClassKind.CLASS, Modality.FINAL)

    // java.util.concurrent.AtomicIntegerFieldUpdater
    val atomicIntFieldUpdaterClassSymbol: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicIntegerFieldUpdater", ClassKind.CLASS, Modality.FINAL)

    val atomicIntNewUpdater: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(
            name = "newUpdater",
            returnType = atomicIntFieldUpdaterClassSymbol.defaultType,
            isStatic = true
        ).apply {
            addValueParameter("tclass", javaLangClass.starProjectedType)
            addValueParameter("fieldName", irBuiltIns.stringType)
        }.symbol

    val atomicIntGet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "get", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntSet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    val atomicIntCompareAndSet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("expect", irBuiltIns.intType)
            addValueParameter("update", irBuiltIns.intType)
        }.symbol

    val atomicIntAddAndGet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "addAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("delta", irBuiltIns.intType)
        }.symbol

    val atomicIntGetAndAdd: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("delta", irBuiltIns.intType)
        }.symbol

    val atomicIntIncrementAndGet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntGetAndIncrement: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntDecrementAndGet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntGetAndDecrement: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntLazySet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    val atomicIntGetAndSet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClassSymbol.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    // java.util.concurrent.AtomicLongFieldUpdater
    val atomicLongFieldUpdaterClassSymbol: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicLongFieldUpdater", ClassKind.CLASS, Modality.FINAL)

    val atomicLongNewUpdater: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(
            name = "newUpdater",
            returnType = atomicLongFieldUpdaterClassSymbol.defaultType,
            isStatic = true
        ).apply {
            addValueParameter("tclass", javaLangClass.starProjectedType)
            addValueParameter("fieldName", irBuiltIns.stringType)
        }.symbol

    val atomicLongGet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "get", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongSet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    val atomicLongCompareAndSet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("expect", irBuiltIns.longType)
            addValueParameter("update", irBuiltIns.longType)
        }.symbol

    val atomicLongAddAndGet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "addAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("delta", irBuiltIns.longType)
        }.symbol

    val atomicLongGetAndAdd: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("delta", irBuiltIns.longType)
        }.symbol

    val atomicLongIncrementAndGet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongGetAndIncrement: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongDecrementAndGet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongGetAndDecrement: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongLazySet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    val atomicLongGetAndSet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClassSymbol.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    // java.util.concurrent.AtomicReferenceFieldUpdater
    val atomicRefFieldUpdaterClassSymbol: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicReferenceFieldUpdater", ClassKind.CLASS, Modality.FINAL)

    val atomicRefNewUpdater: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClassSymbol.owner.addFunction(
            name = "newUpdater",
            returnType = atomicRefFieldUpdaterClassSymbol.defaultType,
            isStatic = true
        ).apply {
            addValueParameter("tclass", javaLangClass.starProjectedType)
            addValueParameter("vclass", javaLangClass.starProjectedType)
            addValueParameter("fieldName", irBuiltIns.stringType)
        }.symbol

    val atomicRefGet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClassSymbol.owner.addFunction(name = "get", returnType = irBuiltIns.anyNType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            returnType = valueType.defaultType
        }.symbol

    val atomicRefSet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClassSymbol.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", valueType.defaultType)
        }.symbol

    val atomicRefCompareAndSet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClassSymbol.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("expect", valueType.defaultType)
            addValueParameter("update", valueType.defaultType)
        }.symbol

    val atomicRefLazySet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClassSymbol.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", valueType.defaultType)
        }.symbol

    val atomicRefGetAndSet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClassSymbol.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.anyNType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", valueType.defaultType)
            returnType = valueType.defaultType
        }.symbol

    // java.util.concurrent.AtomicIntegerArray
    override val atomicIntArrayClassSymbol: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicIntegerArray", ClassKind.CLASS, Modality.FINAL)

    val atomicIntArrayConstructor: IrConstructorSymbol = atomicIntArrayClassSymbol.owner.addConstructor().apply {
        addValueParameter("length", irBuiltIns.intType)
    }.symbol

    val atomicIntArrayGet: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "get", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArraySet: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayCompareAndSet: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("expect", irBuiltIns.intType)
            addValueParameter("update", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayAddAndGet: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "addAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("delta", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayGetAndAdd: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("delta", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayIncrementAndGet: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayGetAndIncrement: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayDecrementAndGet: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayGetAndDecrement: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayLazySet: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayGetAndSet: IrSimpleFunctionSymbol =
        atomicIntArrayClassSymbol.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    // java.util.concurrent.AtomicLongArray
    override val atomicLongArrayClassSymbol: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicLongArray", ClassKind.CLASS, Modality.FINAL)

    val atomicLongArrayConstructor: IrConstructorSymbol = atomicLongArrayClassSymbol.owner.addConstructor().apply {
        addValueParameter("length", irBuiltIns.intType)
    }.symbol

    val atomicLongArrayGet: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "get", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArraySet: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayCompareAndSet: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("expect", irBuiltIns.longType)
            addValueParameter("update", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayAddAndGet: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "addAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("delta", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayGetAndAdd: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("delta", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayIncrementAndGet: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArrayGetAndIncrement: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArrayDecrementAndGet: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArrayGetAndDecrement: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArrayLazySet: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayGetAndSet: IrSimpleFunctionSymbol =
        atomicLongArrayClassSymbol.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    // java.util.concurrent.AtomicReferenceArray
    override val atomicRefArrayClassSymbol: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicReferenceArray", ClassKind.CLASS, Modality.FINAL)

    val atomicRefArrayConstructor: IrConstructorSymbol = atomicRefArrayClassSymbol.owner.addConstructor().apply {
        addValueParameter("length", irBuiltIns.intType)
    }.symbol

    val atomicRefArrayGet: IrSimpleFunctionSymbol =
        atomicRefArrayClassSymbol.owner.addFunction(name = "get", returnType = irBuiltIns.anyNType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            returnType = valueType.defaultType
        }.symbol

    val atomicRefArraySet: IrSimpleFunctionSymbol =
        atomicRefArrayClassSymbol.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", valueType.defaultType)
        }.symbol

    val atomicRefArrayCompareAndSet: IrSimpleFunctionSymbol =
        atomicRefArrayClassSymbol.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("expect", valueType.defaultType)
            addValueParameter("update", valueType.defaultType)
        }.symbol

    val atomicRefArrayLazySet: IrSimpleFunctionSymbol =
        atomicRefArrayClassSymbol.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", valueType.defaultType)
        }.symbol

    val atomicRefArrayGetAndSet: IrSimpleFunctionSymbol =
        atomicRefArrayClassSymbol.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.anyNType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", valueType.defaultType)
            returnType = valueType.defaultType
        }.symbol

    private val ATOMIC_FIELD_UPDATER_TYPES: Set<IrClassSymbol> = setOf(
        atomicIntFieldUpdaterClassSymbol,
        atomicLongFieldUpdaterClassSymbol,
        atomicRefFieldUpdaterClassSymbol
    )

    // TODO use where applicable
    override fun getAtomicHandlerTypeByAtomicfuType(atomicfuType: IrType): IrType {
        require(atomicfuType.classFqName?.parent()?.asString() == "kotlinx.atomicfu") { "Expected kotlinx.atomicfu type, but got ${atomicfuType.render()}" }
        val shortClassName = atomicfuType.type.classFqName?.shortName()?.asString()
            ?: error("Couldn't extract the classFqName for the kotlinx.atomicfu type: ${atomicfuType.render()}")
        return when (shortClassName) {
            ATOMIC_INT -> atomicIntFieldUpdaterClassSymbol
            ATOMIC_BOOLEAN -> atomicIntFieldUpdaterClassSymbol
            ATOMIC_LONG -> atomicLongFieldUpdaterClassSymbol
            ATOMIC_REF -> atomicRefFieldUpdaterClassSymbol
            ATOMIC_INT_ARRAY -> atomicIntArrayClassSymbol
            ATOMIC_BOOLEAN_ARRAY -> atomicIntArrayClassSymbol
            ATOMIC_LONG_ARRAY -> atomicLongArrayClassSymbol
            ATOMIC_ARRAY -> atomicRefArrayClassSymbol
            else -> error("Unexpected name of kotlinx.atomicfu type: ${atomicfuType.render()}")
        }.defaultType
    }

    // only one constructor is imported for java array types (see values atomicIntArrayConstructor, atomicRefArrayConstructor)
    override fun getAtomicArrayConstructor(atomicArrayClassSymbol: IrClassSymbol): IrFunctionSymbol =
        atomicArrayClassSymbol.constructors.firstOrNull() ?: error("No constructors found for ${atomicArrayClassSymbol.owner.render()}")

    override fun createBuilder(symbol: IrSymbol, startOffset: Int, endOffset: Int) =
        JvmAtomicfuIrBuilder(this, symbol, startOffset, endOffset)

    fun javaFUClassSymbol(valueType: IrType): IrClassSymbol =
        when {
            valueType.isInt() -> atomicIntFieldUpdaterClassSymbol
            valueType.isLong() -> atomicLongFieldUpdaterClassSymbol
            valueType.isBoolean() -> atomicIntFieldUpdaterClassSymbol
            !valueType.isPrimitiveType() -> atomicRefFieldUpdaterClassSymbol
            else -> error("The corresponding Java AtomicFieldUpdater class cannot be found for the given value type ${valueType.render()}")
        }

    fun isAtomicFieldUpdaterType(valueType: IrType) = valueType.classOrNull in ATOMIC_FIELD_UPDATER_TYPES

    fun newUpdater(atomicUpdaterClassSymbol: IrClassSymbol): IrSimpleFunctionSymbol =
        atomicUpdaterClassSymbol.getSimpleFunction("newUpdater")
            ?: error("No newUpdater function was found for ${atomicUpdaterClassSymbol.owner.render()} ")

    private val kotlinKClassJava: IrPropertySymbol = irFactory.buildProperty {
        name = Name.identifier("java")
    }.apply {
        parent = kotlinJvm
        addGetter().apply {
            addExtensionReceiver(irBuiltIns.kClassClass.starProjectedType)
            returnType = javaLangClass.defaultType
        }
    }.symbol

    fun kClassToJavaClass(kClassReference: IrExpression): IrCall =
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
        valueArgumentsCount = 0,
        origin = IrStatementOrigin.GET_PROPERTY
    ).apply {
        dispatchReceiver = receiver
    }

    override val volatileAnnotationClass: IrClass
        get() = context.referenceClass(ClassId(FqName("kotlin.jvm"), Name.identifier("Volatile")))?.owner
            ?: error("kotlin.jvm.Volatile class is not found")
}
