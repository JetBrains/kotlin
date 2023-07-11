/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop

import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.KotlinType

internal inline fun <reified T: DeclarationDescriptor> ClassDescriptor.findDeclarationByName(name: String): T? =
        unsubstitutedMemberScope
                .getContributedDescriptors()
                .filterIsInstance<T>()
                .firstOrNull { it.name.identifier == name }

/**
 * Provides a set of functions and properties that helps
 * to translate descriptor declarations to corresponding IR.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal interface DescriptorToIrTranslationMixin {

    val symbolTable: SymbolTable

    val irBuiltIns: IrBuiltIns

    val typeTranslator: TypeTranslator

    val postLinkageSteps: MutableList<() -> Unit>

    fun invokePostLinkageSteps() {
        postLinkageSteps.forEach { it() }
    }

    fun KotlinType.toIrType() = typeTranslator.translateType(this)

    /**
     * Declares [IrClass] instance from [descriptor] and populates it with
     * supertypes, <this> parameter declaration and fake overrides.
     * Additional elements are passed via [builder] callback.
     */
    fun createClass(descriptor: ClassDescriptor, builder: (IrClass) -> Unit): IrClass =
            symbolTable.descriptorExtension.declareClass(descriptor) {
                symbolTable.irFactory.createIrClassFromDescriptor(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, it, descriptor
                )
            }.also { irClass ->
                symbolTable.withScope(irClass) {
                    irClass.superTypes += descriptor.typeConstructor.supertypes.map {
                        it.toIrType()
                    }
                    irClass.generateAnnotations()
                    irClass.createParameterDeclarations()
                    builder(irClass)
                    createFakeOverrides(descriptor).forEach(irClass::addMember)
                }
            }

    private fun createFakeOverrides(classDescriptor: ClassDescriptor): List<IrDeclaration> {
        val fakeOverrides = classDescriptor.unsubstitutedMemberScope
                .getContributedDescriptors()
                .filterIsInstance<CallableMemberDescriptor>()
                .filter { it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE }
        return fakeOverrides.map {
            when (it) {
                is PropertyDescriptor -> createProperty(it)
                is FunctionDescriptor -> createFunction(it)
                else -> error("Unexpected fake override descriptor: $it")
            }
        }
    }

    fun createConstructor(constructorDescriptor: ClassConstructorDescriptor): IrConstructor {
        val irConstructor = symbolTable.descriptorExtension.declareConstructor(constructorDescriptor) {
            with(constructorDescriptor) {
                IrConstructorImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, it, name, visibility,
                    IrUninitializedType, isInline, isEffectivelyExternal(), isPrimary, isExpect
                )
            }
        }
        irConstructor.valueParameters += constructorDescriptor.valueParameters.map { valueParameterDescriptor ->
            symbolTable.descriptorExtension.declareValueParameter(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.DEFINED,
                    valueParameterDescriptor,
                    valueParameterDescriptor.type.toIrType()).also {
                it.parent = irConstructor
            }
        }
        irConstructor.returnType = constructorDescriptor.returnType.toIrType()
        irConstructor.generateAnnotations()
        return irConstructor
    }

    fun createProperty(propertyDescriptor: PropertyDescriptor): IrProperty {
        val irProperty = symbolTable.descriptorExtension.declareProperty(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, propertyDescriptor, propertyDescriptor.isDelegated)
        irProperty.getter = propertyDescriptor.getter?.let {
            val irGetter = createFunction(it)
            irGetter.correspondingPropertySymbol = irProperty.symbol
            irGetter
        }
        irProperty.setter = propertyDescriptor.setter?.let {
            val irSetter = createFunction(it)
            irSetter.correspondingPropertySymbol = irProperty.symbol
            irSetter
        }
        irProperty.generateAnnotations()
        return irProperty
    }

    fun createFunction(functionDescriptor: FunctionDescriptor): IrSimpleFunction {
        val irFunction = symbolTable.declareSimpleFunctionWithOverrides(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB, functionDescriptor)
        symbolTable.withScope(irFunction) {
            irFunction.returnType = functionDescriptor.returnType!!.toIrType()
            irFunction.valueParameters +=  functionDescriptor.valueParameters.map {
                symbolTable.descriptorExtension.declareValueParameter(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.DEFINED, it, it.type.toIrType())
            }
            irFunction.dispatchReceiverParameter = functionDescriptor.dispatchReceiverParameter?.let {
                symbolTable.descriptorExtension.declareValueParameter(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.DEFINED, it, it.type.toIrType())
            }
            irFunction.generateAnnotations()
        }
        return irFunction
    }

    private fun IrDeclaration.generateAnnotations() {
        annotations += descriptor.annotations.map {
            typeTranslator.constantValueGenerator.generateAnnotationConstructorCall(it)
                ?: error("Could not generate annotations for $it")
        }
    }
}

internal fun IrBuilder.irInstanceInitializer(classSymbol: IrClassSymbol): IrExpression =
        IrInstanceInitializerCallImpl(
                startOffset, endOffset,
                classSymbol,
                context.irBuiltIns.unitType
        )

internal fun ClassDescriptor.implementsCEnum(): Boolean =
        getSuperInterfaces().any { it.fqNameSafe == InteropFqNames.cEnum }

internal fun ClassDescriptor.inheritsFromCStructVar(): Boolean =
        getSuperClassNotAny()?.fqNameSafe == InteropFqNames.cStructVar

/**
 * All enums that come from interop library implement CEnum interface.
 * This function checks that given symbol located in subtree of
 * CEnum inheritor.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun IrSymbol.findCEnumDescriptor(): ClassDescriptor? =
        descriptor.findCEnumDescriptor()

internal fun DeclarationDescriptor.findCEnumDescriptor(): ClassDescriptor? =
        parentsWithSelf.filterIsInstance<ClassDescriptor>().firstOrNull { it.implementsCEnum() }

/**
 * All structs that come from interop library inherit from CStructVar class.
 * This function checks that given symbol located in subtree of
 * CStructVar inheritor.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal fun IrSymbol.findCStructDescriptor(): ClassDescriptor? =
        descriptor.findCStructDescriptor()

internal fun DeclarationDescriptor.findCStructDescriptor(): ClassDescriptor? =
        parentsWithSelf.filterIsInstance<ClassDescriptor>().firstOrNull {
            it.inheritsFromCStructVar() || it.annotations.hasAnnotation(RuntimeNames.managedType)
        }
