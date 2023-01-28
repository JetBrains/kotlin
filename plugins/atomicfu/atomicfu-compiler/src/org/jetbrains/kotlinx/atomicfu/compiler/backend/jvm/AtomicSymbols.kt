/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlinx.atomicfu.compiler.backend.*

// Contains IR declarations needed by the atomicfu plugin.
class AtomicSymbols(
    val irBuiltIns: IrBuiltIns,
    private val moduleFragment: IrModuleFragment
) {
    private val irFactory: IrFactory = IrFactoryImpl
    private val javaLang: IrPackageFragment = createPackage("java.lang")
    private val javaUtilConcurrent: IrPackageFragment = createPackage("java.util.concurrent.atomic")
    private val kotlinJvm: IrPackageFragment = createPackage("kotlin.jvm")
    private val javaLangClass: IrClassSymbol = createClass(javaLang, "Class", ClassKind.CLASS, Modality.FINAL)

    // AtomicIntegerFieldUpdater
    val atomicIntFieldUpdaterClass: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicIntegerFieldUpdater", ClassKind.CLASS, Modality.FINAL)

    val atomicIntNewUpdater: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(
            name = "newUpdater",
            returnType = atomicIntFieldUpdaterClass.defaultType,
            isStatic = true
        ).apply {
            addValueParameter("tclass", javaLangClass.starProjectedType)
            addValueParameter("fieldName", irBuiltIns.stringType)
        }.symbol

    val atomicIntGet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "get", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntSet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    val atomicIntCompareAndSet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("expect", irBuiltIns.intType)
            addValueParameter("update", irBuiltIns.intType)
        }.symbol

    val atomicIntAddAndGet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "addAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("delta", irBuiltIns.intType)
        }.symbol

    val atomicIntGetAndAdd: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("delta", irBuiltIns.intType)
        }.symbol

    val atomicIntIncrementAndGet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntGetAndIncrement: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntDecrementAndGet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntGetAndDecrement: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicIntLazySet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    val atomicIntGetAndSet: IrSimpleFunctionSymbol =
        atomicIntFieldUpdaterClass.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.intType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    // AtomicLongFieldUpdater
    val atomicLongFieldUpdaterClass: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicLongFieldUpdater", ClassKind.CLASS, Modality.FINAL)

    val atomicLongNewUpdater: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(
            name = "newUpdater",
            returnType = atomicLongFieldUpdaterClass.defaultType,
            isStatic = true
        ).apply {
            addValueParameter("tclass", javaLangClass.starProjectedType)
            addValueParameter("fieldName", irBuiltIns.stringType)
        }.symbol

    val atomicLongGet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "get", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongSet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    val atomicLongCompareAndSet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("expect", irBuiltIns.longType)
            addValueParameter("update", irBuiltIns.longType)
        }.symbol

    val atomicLongAddAndGet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "addAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("delta", irBuiltIns.longType)
        }.symbol

    val atomicLongGetAndAdd: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("delta", irBuiltIns.longType)
        }.symbol

    val atomicLongIncrementAndGet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongGetAndIncrement: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongDecrementAndGet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongGetAndDecrement: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
        }.symbol

    val atomicLongLazySet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    val atomicLongGetAndSet: IrSimpleFunctionSymbol =
        atomicLongFieldUpdaterClass.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.longType).apply {
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    // AtomicReferenceFieldUpdater
    val atomicRefFieldUpdaterClass: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicReferenceFieldUpdater", ClassKind.CLASS, Modality.FINAL)

    val atomicRefNewUpdater: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClass.owner.addFunction(
            name = "newUpdater",
            returnType = atomicRefFieldUpdaterClass.defaultType,
            isStatic = true
        ).apply {
            addValueParameter("tclass", javaLangClass.starProjectedType)
            addValueParameter("vclass", javaLangClass.starProjectedType)
            addValueParameter("fieldName", irBuiltIns.stringType)
        }.symbol

    val atomicRefGet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClass.owner.addFunction(name = "get", returnType = irBuiltIns.anyNType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            returnType = valueType.defaultType
        }.symbol

    val atomicRefSet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClass.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", valueType.defaultType)
        }.symbol

    val atomicRefCompareAndSet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClass.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("expect", valueType.defaultType)
            addValueParameter("update", valueType.defaultType)
        }.symbol

    val atomicRefLazySet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClass.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", valueType.defaultType)
        }.symbol

    val atomicRefGetAndSet: IrSimpleFunctionSymbol =
        atomicRefFieldUpdaterClass.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.anyNType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("obj", irBuiltIns.anyType)
            addValueParameter("newValue", valueType.defaultType)
            returnType = valueType.defaultType
        }.symbol

    // AtomicIntegerArray
    val atomicIntArrayClass: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicIntegerArray", ClassKind.CLASS, Modality.FINAL)

    val atomicIntArrayConstructor: IrConstructorSymbol = atomicIntArrayClass.owner.addConstructor().apply {
        addValueParameter("length", irBuiltIns.intType)
    }.symbol

    val atomicIntArrayGet: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "get", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArraySet: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayCompareAndSet: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("expect", irBuiltIns.intType)
            addValueParameter("update", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayAddAndGet: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "addAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("delta", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayGetAndAdd: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("delta", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayIncrementAndGet: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayGetAndIncrement: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayDecrementAndGet: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayGetAndDecrement: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayLazySet: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    val atomicIntArrayGetAndSet: IrSimpleFunctionSymbol =
        atomicIntArrayClass.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.intType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.intType)
        }.symbol

    // AtomicLongArray
    val atomicLongArrayClass: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicLongArray", ClassKind.CLASS, Modality.FINAL)

    val atomicLongArrayConstructor: IrConstructorSymbol = atomicLongArrayClass.owner.addConstructor().apply {
        addValueParameter("length", irBuiltIns.intType)
    }.symbol

    val atomicLongArrayGet: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "get", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArraySet: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayCompareAndSet: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("expect", irBuiltIns.longType)
            addValueParameter("update", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayAddAndGet: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "addAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("delta", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayGetAndAdd: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "getAndAdd", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("delta", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayIncrementAndGet: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "incrementAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArrayGetAndIncrement: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "getAndIncrement", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArrayDecrementAndGet: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "decrementAndGet", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArrayGetAndDecrement: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "getAndDecrement", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
        }.symbol

    val atomicLongArrayLazySet: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    val atomicLongArrayGetAndSet: IrSimpleFunctionSymbol =
        atomicLongArrayClass.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.longType).apply {
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", irBuiltIns.longType)
        }.symbol

    // AtomicReferenceArray
    val atomicRefArrayClass: IrClassSymbol =
        createClass(javaUtilConcurrent, "AtomicReferenceArray", ClassKind.CLASS, Modality.FINAL)

    val atomicRefArrayConstructor: IrConstructorSymbol = atomicRefArrayClass.owner.addConstructor().apply {
        addValueParameter("length", irBuiltIns.intType)
    }.symbol

    val atomicRefArrayGet: IrSimpleFunctionSymbol =
        atomicRefArrayClass.owner.addFunction(name = "get", returnType = irBuiltIns.anyNType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            returnType = valueType.defaultType
        }.symbol

    val atomicRefArraySet: IrSimpleFunctionSymbol =
        atomicRefArrayClass.owner.addFunction(name = "set", returnType = irBuiltIns.unitType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", valueType.defaultType)
        }.symbol

    val atomicRefArrayCompareAndSet: IrSimpleFunctionSymbol =
        atomicRefArrayClass.owner.addFunction(name = "compareAndSet", returnType = irBuiltIns.booleanType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("expect", valueType.defaultType)
            addValueParameter("update", valueType.defaultType)
        }.symbol

    val atomicRefArrayLazySet: IrSimpleFunctionSymbol =
        atomicRefArrayClass.owner.addFunction(name = "lazySet", returnType = irBuiltIns.unitType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", valueType.defaultType)
        }.symbol

    val atomicRefArrayGetAndSet: IrSimpleFunctionSymbol =
        atomicRefArrayClass.owner.addFunction(name = "getAndSet", returnType = irBuiltIns.anyNType).apply {
            val valueType = addTypeParameter("T", irBuiltIns.anyNType)
            addValueParameter("i", irBuiltIns.intType)
            addValueParameter("newValue", valueType.defaultType)
            returnType = valueType.defaultType
        }.symbol

    private val VALUE_TYPE_TO_ATOMIC_ARRAY_CLASS: Map<IrType, IrClassSymbol> = mapOf(
        irBuiltIns.intType to atomicIntArrayClass,
        irBuiltIns.booleanType to atomicIntArrayClass,
        irBuiltIns.longType to atomicLongArrayClass,
        irBuiltIns.anyNType to atomicRefArrayClass
    )

    private val ATOMIC_ARRAY_TYPES: Set<IrClassSymbol> = setOf(
        atomicIntArrayClass,
        atomicLongArrayClass,
        atomicRefArrayClass
    )

    private val ATOMIC_FIELD_UPDATER_TYPES: Set<IrClassSymbol> = setOf(
        atomicIntFieldUpdaterClass,
        atomicLongFieldUpdaterClass,
        atomicRefFieldUpdaterClass
    )

    fun getJucaAFUClass(valueType: IrType): IrClassSymbol =
        when {
            valueType.isInt() -> atomicIntFieldUpdaterClass
            valueType.isLong() -> atomicLongFieldUpdaterClass
            valueType.isBoolean() -> atomicIntFieldUpdaterClass
            else -> atomicRefFieldUpdaterClass
        }

    fun getFieldUpdaterType(valueType: IrType) = getJucaAFUClass(valueType).defaultType

    fun getAtomicArrayClassByAtomicfuArrayType(atomicfuArrayType: IrType): IrClassSymbol =
        when (atomicfuArrayType.classFqName?.shortName()?.asString()) {
            "AtomicIntArray" -> atomicIntArrayClass
            "AtomicLongArray" -> atomicLongArrayClass
            "AtomicBooleanArray" -> atomicIntArrayClass
            "AtomicArray" -> atomicRefArrayClass
            else -> error("Unexpected atomicfu array type ${atomicfuArrayType.render()}")
        }

    fun getAtomicArrayClassByValueType(valueType: IrType): IrClassSymbol =
        VALUE_TYPE_TO_ATOMIC_ARRAY_CLASS[valueType]
            ?: error("No corresponding atomic array class found for this value type ${valueType.render()} ")

    fun getAtomicArrayType(valueType: IrType) = getAtomicArrayClassByValueType(valueType).defaultType

    fun isAtomicArrayHandlerType(valueType: IrType) = valueType.classOrNull in ATOMIC_ARRAY_TYPES

    fun isAtomicFieldUpdaterType(valueType: IrType) = valueType.classOrNull in ATOMIC_FIELD_UPDATER_TYPES

    fun getNewUpdater(atomicUpdaterClassSymbol: IrClassSymbol): IrSimpleFunctionSymbol =
        atomicUpdaterClassSymbol.getSimpleFunction("newUpdater") ?: error("No newUpdater function was found for ${atomicUpdaterClassSymbol.owner.render()} ")

    fun getAtomicArrayConstructor(atomicArrayClassSymbol: IrClassSymbol): IrConstructorSymbol =
        atomicArrayClassSymbol.constructors.firstOrNull() ?: error("No constructors declared for ${atomicArrayClassSymbol.owner.render()} ")

    fun getAtomicHandlerFunctionSymbol(atomicHandlerClass: IrClassSymbol, name: String): IrSimpleFunctionSymbol =
        when (name) {
            "<get-value>", "getValue" -> atomicHandlerClass.getSimpleFunction("get")
            "<set-value>", "setValue" -> atomicHandlerClass.getSimpleFunction("set")
            else -> atomicHandlerClass.getSimpleFunction(name)
        } ?: error("No $name function found in $name")

    val kotlinKClassJava: IrPropertySymbol = irFactory.buildProperty {
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

    fun function0Type(returnType: IrType) = buildFunctionSimpleType(
        irBuiltIns.functionN(0).symbol,
        listOf(returnType)
    )

    fun function1Type(argType: IrType, returnType: IrType) = buildFunctionSimpleType(
        irBuiltIns.functionN(1).symbol,
        listOf(argType, returnType)
    )

    val invoke0Symbol = irBuiltIns.functionN(0).getSimpleFunction("invoke")!!
    val invoke1Symbol = irBuiltIns.functionN(1).getSimpleFunction("invoke")!!

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

    private val volatileConstructor = buildAnnotationConstructor(buildClass(JvmNames.VOLATILE_ANNOTATION_FQ_NAME, ClassKind.ANNOTATION_CLASS, kotlinJvm))
    val volatileAnnotationConstructorCall =
        IrConstructorCallImpl.fromSymbolOwner(volatileConstructor.returnType, volatileConstructor.symbol)

    fun buildClass(
        fqName: FqName,
        classKind: ClassKind,
        parent: IrDeclarationContainer
    ): IrClass = irFactory.buildClass {
        name = fqName.shortName()
        kind = classKind
    }.apply {
        val irClass = this
        this.parent = parent
        parent.addChild(irClass)
        thisReceiver = buildValueParameter(irClass) {
            name = Name.identifier("\$this")
            type = IrSimpleTypeImpl(irClass.symbol, false, emptyList(), emptyList())
        }
    }

    private fun buildAnnotationConstructor(annotationClass: IrClass): IrConstructor =
        annotationClass.addConstructor { isPrimary = true }

    private fun createPackage(packageName: String): IrPackageFragment =
        IrExternalPackageFragmentImpl.createEmptyExternalPackageFragment(
            moduleFragment.descriptor,
            FqName(packageName)
        )

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
        createImplicitParameterDeclarationWithWrappedDescriptor()
    }.symbol

    fun createBuilder(
        symbol: IrSymbol,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ) = AtomicfuIrBuilder(this, symbol, startOffset, endOffset)
}
