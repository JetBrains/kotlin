/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend.jvm

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.atomicfu.compiler.backend.AtomicHandlerType
import org.jetbrains.kotlinx.atomicfu.compiler.backend.atomicfuRender
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicfuIrBuilder
import org.jetbrains.kotlinx.atomicfu.compiler.backend.common.AbstractAtomicSymbols
import org.jetbrains.kotlinx.atomicfu.compiler.diagnostic.AtomicfuErrorMessages.CONSTRAINTS_MESSAGE

class JvmAtomicfuIrBuilder(
    override val atomicfuSymbols: JvmAtomicSymbols,
    symbol: IrSymbol,
): AbstractAtomicfuIrBuilder(atomicfuSymbols.irBuiltIns, symbol) {

    override fun irCallFunction(
        symbol: IrSimpleFunctionSymbol,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        valueArguments: List<IrExpression?>,
        valueType: IrType
    ): IrCall {
        val irCall = irCall(symbol).apply {
            this.dispatchReceiver = dispatchReceiver
            this.extensionReceiver = extensionReceiver
            valueArguments.forEachIndexed { i, arg ->
                putValueArgument(i, arg?.let {
                    val expectedParameterType = symbol.owner.valueParameters[i].type
                    if (valueType.isBoolean() && !arg.type.isInt() && expectedParameterType.isInt()) toInt(it) else it
                })
            }
        }
        return if (valueType.isBoolean() && symbol.owner.returnType.isInt()) toBoolean(irCall) else irCall
    }

    override fun invokeFunctionOnAtomicHandler(
        atomicHandlerType: AtomicHandlerType,
        getAtomicHandler: IrExpression,
        functionName: String,
        valueArguments: List<IrExpression?>,
        valueType: IrType,
    ): IrCall {
        require(
            atomicHandlerType == AtomicHandlerType.ATOMIC_ARRAY ||
                    atomicHandlerType == AtomicHandlerType.ATOMIC_FIELD_UPDATER ||
                    atomicHandlerType == AtomicHandlerType.BOXED_ATOMIC
        ) { "Unexpected atomic handler type: $atomicHandlerType for the JVM backend." }
        return invokeFunctionOnAtomicHandlerClass(getAtomicHandler, functionName, valueArguments, valueType)
    }

    override fun buildVolatileFieldOfType(
        name: String,
        valueType: IrType,
        annotations: List<IrConstructorCall>,
        initExpr: IrExpression?,
        parentContainer: IrDeclarationContainer
    ): IrField {
        // On JVM a volatile Int field is generated to replace an AtomicBoolean property
        val castBooleanToInt = valueType.isBoolean()
        val volatileFieldType = if (castBooleanToInt) irBuiltIns.intType else valueType
        return irVolatileField(name, volatileFieldType, annotations, parentContainer).apply {
            if (initExpr != null) {
                this.initializer = irExprBody(if (castBooleanToInt) toInt(initExpr) else initExpr)
            }
        }
    }

    fun irBoxedAtomicField(atomicProperty: IrProperty, parentContainer: IrDeclarationContainer): IrField {
        val atomicfuField = requireNotNull(atomicProperty.backingField) {
            "The backing field of the atomic property ${atomicProperty.atomicfuRender()} declared in ${parentContainer.render()} should not be null." + CONSTRAINTS_MESSAGE
        }
        return buildAndInitializeNewField(atomicfuField, parentContainer) { atomicFactoryCall: IrExpression ->
            val initValue = atomicFactoryCall.getAtomicFactoryValueArgument()
            val valueType = atomicfuSymbols.atomicToPrimitiveType(atomicfuField.type as IrSimpleType)
            val atomicBoxType = atomicfuSymbols.javaAtomicBoxClassSymbol(valueType)
            context.irFactory.buildField {
                this.name = atomicfuField.name
                type = atomicBoxType.defaultType
                this.isFinal = true
                this.isStatic = atomicfuField.isStatic
                visibility = DescriptorVisibilities.PRIVATE
                origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
            }.apply {
                this.initializer = context.irFactory.createExpressionBody(
                    newJavaBoxedAtomic(atomicBoxType, initValue, (atomicFactoryCall as IrFunctionAccessExpression).dispatchReceiver)
                )
                this.annotations = annotations
                this.parent = parentContainer
            }
        }
    }

    fun irJavaAtomicFieldUpdater(volatileField: IrField, parentClass: IrClass): IrField {
        // Generate an atomic field updater for the volatile backing field of the given property:
        // val a = atomic(0)
        // volatile var a: Int = 0
        // val a$FU = AtomicIntegerFieldUpdater.newUpdater(parentClass, "a")
        val fuClass = atomicfuSymbols.javaFUClassSymbol(volatileField.type)
        val fieldName = volatileField.name.asString()
        return context.irFactory.buildField {
            name = Name.identifier("$fieldName\$FU")
            type = fuClass.defaultType
            isFinal = true
            isStatic = true
            visibility = DescriptorVisibilities.PRIVATE
            origin = AbstractAtomicSymbols.ATOMICFU_GENERATED_FIELD
        }.apply {
            initializer = irExprBody(newJavaAtomicFieldUpdater(fuClass, parentClass, irBuiltIns.anyNType, fieldName))
            parent = parentClass
        }
    }

    private fun newJavaBoxedAtomic(
        atomicBoxType: IrClassSymbol,
        initValue: IrExpression,
        dispatchReceiver: IrExpression?
    ) : IrFunctionAccessExpression = irCall(atomicBoxType.constructors.first()).apply {
        putValueArgument(0, initValue)
        this.dispatchReceiver = dispatchReceiver
    }

    // val a$FU = j.u.c.a.AtomicIntegerFieldUpdater.newUpdater(A::class, "a")
    private fun newJavaAtomicFieldUpdater(
        fieldUpdaterClass: IrClassSymbol,
        parentClass: IrClass,
        valueType: IrType,
        fieldName: String
    ) = irCall(atomicfuSymbols.newUpdater(fieldUpdaterClass)).apply {
        putValueArgument(0, atomicfuSymbols.javaClassReference(parentClass.symbol.starProjectedType)) // tclass
        if (fieldUpdaterClass == atomicfuSymbols.javaAtomicRefFieldUpdaterClass) {
            putValueArgument(1, atomicfuSymbols.javaClassReference(valueType)) // vclass
            putValueArgument(2, irString(fieldName)) // fieldName
        } else {
            putValueArgument(1, irString(fieldName)) // fieldName
        }
    }

    override fun newAtomicArray(
        atomicArrayClass: IrClassSymbol,
        size: IrExpression,
        valueType: IrType,
        dispatchReceiver: IrExpression?
    ) = callArraySizeConstructor(atomicArrayClass, size, dispatchReceiver)
        ?: error("Failed to find a constructor for the the given atomic array type ${atomicArrayClass.defaultType.render()}.")
}