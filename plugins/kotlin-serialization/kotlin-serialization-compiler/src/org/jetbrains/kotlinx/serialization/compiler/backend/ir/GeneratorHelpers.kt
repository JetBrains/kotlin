/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializableProperty

val BackendContext.externalSymbols: ReferenceSymbolTable get() = ir.symbols.externalSymbolTable

interface IrBuilderExtension {
    val compilerContext: BackendContext
    val translator: TypeTranslator

    val BackendContext.localSymbolTable: SymbolTable

    fun IrClass.contributeFunction(descriptor: FunctionDescriptor, fromStubs: Boolean = false, bodyGen: IrBlockBodyBuilder.(IrFunction) -> Unit) {
        val f: IrSimpleFunction = if (!fromStubs) compilerContext.localSymbolTable.declareSimpleFunctionWithOverrides(
            this.startOffset,
            this.endOffset,
            SERIALIZABLE_PLUGIN_ORIGIN,
            descriptor
        ) else compilerContext.externalSymbols.referenceSimpleFunction(descriptor).owner
        f.parent = this
        f.returnType = descriptor.returnType!!.toIrType()
        f.createParameterDeclarations()
        f.body = compilerContext.createIrBuilder(f.symbol).irBlockBody { bodyGen(f) }
        this.addMember(f)
    }

    fun IrClass.contributeConstructor(
        descriptor: ClassConstructorDescriptor,
        bodyGen: IrBlockBodyBuilder.(IrConstructor) -> Unit
    ) {
        val c = compilerContext.localSymbolTable.declareConstructor(
            this.startOffset,
            this.endOffset,
            SERIALIZABLE_PLUGIN_ORIGIN,
            descriptor
        )
        c.parent = this
        c.returnType = descriptor.returnType.toIrType()
        c.createParameterDeclarations()
        c.body = compilerContext.createIrBuilder(c.symbol).irBlockBody { bodyGen(c) }
        this.addMember(c)
    }

    fun IrBuilderWithScope.irInvoke(
        dispatchReceiver: IrExpression? = null,
        callee: IrFunctionSymbol,
        vararg args: IrExpression,
        typeHint: IrType? = null
    ): IrCall {
        val call = typeHint?.let { irCall(callee, type = it) } ?: irCall(callee)
        call.dispatchReceiver = dispatchReceiver
        args.forEachIndexed(call::putValueArgument)
        return call
    }

    fun IrBuilderWithScope.irBinOp(name: Name, lhs: IrExpression, rhs: IrExpression): IrExpression {
        val symbol = compilerContext.ir.symbols.getBinaryOperator(
            name,
            lhs.type.toKotlinType(),
            rhs.type.toKotlinType()
        )
        return irInvoke(lhs, symbol, rhs)
    }

    fun IrBuilderWithScope.irGetObject(classDescriptor: ClassDescriptor) =
        IrGetObjectValueImpl(
            startOffset,
            endOffset,
            classDescriptor.defaultType.toIrType(),
            compilerContext.externalSymbols.referenceClass(classDescriptor)
        )

    fun IrBuilderWithScope.irGetObject(irObject: IrClass) =
        IrGetObjectValueImpl(
            startOffset,
            endOffset,
            irObject.defaultType,
            irObject.symbol
        )

    fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            compilerContext.localSymbolTable.withScope(irDeclaration.descriptor) {
                builder(irDeclaration)
            }
        }

    fun IrBuilderWithScope.irEmptyVararg(forValueParameter: ValueParameterDescriptor) =
        IrVarargImpl(
            startOffset,
            endOffset,
            forValueParameter.type.toIrType(),
            forValueParameter.varargElementType!!.toIrType()
        )

    class BranchBuilder(
        val irWhen: IrWhen,
        context: IrGeneratorContext,
        scope: Scope,
        startOffset: Int,
        endOffset: Int
    ) : IrBuilderWithScope(context, scope, startOffset, endOffset) {
        operator fun IrBranch.unaryPlus() {
            irWhen.branches.add(this)
        }
    }

    fun IrBuilderWithScope.irWhen(typeHint: IrType? = null, block: BranchBuilder.() -> Unit): IrWhen {
        val whenExpr = IrWhenImpl(startOffset, endOffset, typeHint ?: compilerContext.irBuiltIns.unitType)
        val builder = BranchBuilder(whenExpr, context, scope, startOffset, endOffset)
        builder.block()
        return whenExpr
    }

    fun BranchBuilder.elseBranch(result: IrExpression): IrElseBranch =
        IrElseBranchImpl(
            IrConstImpl.boolean(result.startOffset, result.endOffset, compilerContext.irBuiltIns.booleanType, true),
            result
        )

    fun translateType(ktType: KotlinType): IrType =
        translator.translateType(ktType)

    fun KotlinType.toIrType() = translateType(this)


    val SerializableProperty.irField: IrField get() = compilerContext.externalSymbols.referenceField(this.descriptor).owner
//        get () {
//            val symb = compilerContext.localSymbolTable.referenceField(this.descriptor)
//            return if (symb.isBound) symb.owner
//            else compilerContext.localSymbolTable.declareField()
//        }

    /*
     The rest of the file is mainly copied from FunctionGenerator.
     However, I can't use it's directly because all generateSomething methods require KtProperty (psi element)
     Also, FunctionGenerator itself has DeclarationGenerator as ctor param, which is a part of psi2ir
     (it can be instantiated here, but I don't know how good is that idea)
     */

    fun IrBuilderWithScope.generateAnySuperConstructorCall(toBuilder: IrBlockBodyBuilder) {
        val anyConstructor = compilerContext.builtIns.any.constructors.single()
        with(toBuilder) {
            +IrDelegatingConstructorCallImpl(
                startOffset, endOffset,
                compilerContext.irBuiltIns.unitType,
                compilerContext.externalSymbols.referenceConstructor(anyConstructor),
                anyConstructor
            )
        }
    }

    fun generateSimplePropertyWithBackingField(
        ownerSymbol: IrValueSymbol,
        propertyDescriptor: PropertyDescriptor,
        propertyParent: IrClass
    ): IrProperty {
        val irProperty = IrPropertyImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            SERIALIZABLE_PLUGIN_ORIGIN, false,
            propertyDescriptor
        )
        irProperty.parent = propertyParent
        irProperty.backingField = generatePropertyBackingField(propertyDescriptor).apply { parent = propertyParent }
        val fieldSymbol = irProperty.backingField!!.symbol
        irProperty.getter = propertyDescriptor.getter?.let { generatePropertyAccessor(it, fieldSymbol, ownerSymbol) }
            ?.apply { parent = propertyParent }
        irProperty.setter = propertyDescriptor.setter?.let { generatePropertyAccessor(it, fieldSymbol, ownerSymbol) }
            ?.apply { parent = propertyParent }
        return irProperty
    }

    fun generatePropertyBackingField(propertyDescriptor: PropertyDescriptor): IrField {
        return compilerContext.localSymbolTable.declareField(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            SERIALIZABLE_PLUGIN_ORIGIN,
            propertyDescriptor,
            propertyDescriptor.type.toIrType()
        )
    }

    fun generatePropertyAccessor(
        descriptor: PropertyAccessorDescriptor,
        fieldSymbol: IrFieldSymbol,
        ownerSymbol: IrValueSymbol
    ): IrSimpleFunction {
        return compilerContext.localSymbolTable.declareSimpleFunctionWithOverrides(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            SERIALIZABLE_PLUGIN_ORIGIN, descriptor
        ).buildWithScope { irAccessor ->
            irAccessor.createParameterDeclarations((ownerSymbol as IrValueParameterSymbol).owner) // todo: neat this
            irAccessor.returnType = irAccessor.descriptor.returnType!!.toIrType()
            irAccessor.body = when (descriptor) {
                is PropertyGetterDescriptor -> generateDefaultGetterBody(descriptor, irAccessor, ownerSymbol)
                is PropertySetterDescriptor -> generateDefaultSetterBody(descriptor, irAccessor, ownerSymbol)
                else -> throw AssertionError("Should be getter or setter: $descriptor")
            }
        }

    }

    private fun generateDefaultGetterBody(
        getter: PropertyGetterDescriptor,
        irAccessor: IrSimpleFunction,
        ownerSymbol: IrValueSymbol
    ): IrBlockBody {
        val property = getter.correspondingProperty

        val irBody = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)

        val receiver = generateReceiverExpressionForFieldAccess(ownerSymbol, property)

        irBody.statements.add(
            IrReturnImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, compilerContext.irBuiltIns.nothingType,
                irAccessor.symbol,
                IrGetFieldImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    compilerContext.localSymbolTable.referenceField(property),
                    property.type.toIrType(),
                    receiver
                )
            )
        )
        return irBody
    }

    private fun generateDefaultSetterBody(
        setter: PropertySetterDescriptor,
        irAccessor: IrSimpleFunction,
        ownerSymbol: IrValueSymbol
    ): IrBlockBody {
        val property = setter.correspondingProperty

        val startOffset = UNDEFINED_OFFSET
        val endOffset = UNDEFINED_OFFSET
        val irBody = IrBlockBodyImpl(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(ownerSymbol, property)

        val irValueParameter = irAccessor.valueParameters.single()
        irBody.statements.add(
            IrSetFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                compilerContext.localSymbolTable.referenceField(property),
                receiver,
                IrGetValueImpl(startOffset, endOffset, irValueParameter.type, irValueParameter.symbol),
                compilerContext.irBuiltIns.unitType
            )
        )
        return irBody
    }

    fun generateReceiverExpressionForFieldAccess(
        ownerSymbol: IrValueSymbol,
        property: PropertyDescriptor
    ): IrExpression {
        val containingDeclaration = property.containingDeclaration
        return when (containingDeclaration) {
            is ClassDescriptor ->
                IrGetValueImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
//                symbolTable.referenceValue(containingDeclaration.thisAsReceiverParameter)
                    ownerSymbol
                )
            else -> throw AssertionError("Property must be in class")
        }
    }

    // todo: delet zis
    fun IrFunction.createParameterDeclarations(receiver: IrValueParameter? = null) {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            SERIALIZABLE_PLUGIN_ORIGIN,
            this,
            type.toIrType(),
            (this as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = receiver ?: (descriptor.dispatchReceiverParameter?.irValueParameter())
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        assert(valueParameters.isEmpty())
        descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }

        assert(typeParameters.isEmpty())
        descriptor.typeParameters.mapTo(typeParameters) {
            IrTypeParameterImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                SERIALIZABLE_PLUGIN_ORIGIN,
                it
            ).also { typeParameter ->
                typeParameter.parent = this
            }
        }
    }
}
