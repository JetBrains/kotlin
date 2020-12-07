/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.konan.descriptors.findPackage
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions

internal object DECLARATION_ORIGIN_FUNCTION_CLASS : IrDeclarationOriginImpl("DECLARATION_ORIGIN_FUNCTION_CLASS")

internal class BuiltInFictitiousFunctionIrClassFactory(
        var symbolTable: SymbolTable?,
        private val irBuiltIns: IrBuiltIns,
        private val reflectionTypes: KonanReflectionTypes
) : IrAbstractFunctionFactory(), IrProvider {

    override fun getDeclaration(symbol: IrSymbol) =
            (symbol.descriptor as? FunctionClassDescriptor)?.let { descriptor ->
                buildClass(descriptor) {
                    declareClass(descriptor) {
                        createIrClass(it, descriptor)
                    }
                }
            }

    var module: IrModuleFragment? = null
        set(value) {
            if (value == null)
                error("Provide a valid non-null module")
            if (field != null)
                error("Module has already been set")
            field = value
            value.files += filesMap.values
//            builtClasses.forEach { it.addFakeOverrides() }
        }

    class FunctionalInterface(val irClass: IrClass, val arity: Int)

    fun buildAllClasses() {
        val maxArity = 255 // See [BuiltInFictitiousFunctionClassFactory].
        (0 .. maxArity).forEach { arity ->
            functionN(arity)
            kFunctionN(arity)
            suspendFunctionN(arity)
            kSuspendFunctionN(arity)
        }
    }

    override fun functionClassDescriptor(arity: Int): FunctionClassDescriptor =
            irBuiltIns.builtIns.getFunction(arity) as FunctionClassDescriptor

    override fun kFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
            reflectionTypes.getKFunction(arity) as FunctionClassDescriptor

    override fun suspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
            irBuiltIns.builtIns.getSuspendFunction(arity) as FunctionClassDescriptor

    override fun kSuspendFunctionClassDescriptor(arity: Int): FunctionClassDescriptor =
            reflectionTypes.getKSuspendFunction(arity) as FunctionClassDescriptor

    override fun functionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(irBuiltIns.builtIns.getFunction(arity) as FunctionClassDescriptor, declarator)

    override fun kFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(reflectionTypes.getKFunction(arity) as FunctionClassDescriptor, declarator)

    override fun suspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(irBuiltIns.builtIns.getSuspendFunction(arity) as FunctionClassDescriptor, declarator)

    override fun kSuspendFunctionN(arity: Int, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            buildClass(reflectionTypes.getKSuspendFunction(arity) as FunctionClassDescriptor, declarator)

    private val functionSymbol = symbolTable!!.referenceClass(
            irBuiltIns.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(KonanFqNames.function))!!)

    private val kFunctionSymbol = symbolTable!!.referenceClass(
            irBuiltIns.builtIns.builtInsModule.findClassAcrossModuleDependencies(
                    ClassId.topLevel(KonanFqNames.kFunction))!!)

    private val filesMap = mutableMapOf<PackageFragmentDescriptor, IrFile>()

    private val builtClassesMap = mutableMapOf<FunctionClassDescriptor, IrClass>()

    val builtClasses get() = builtClassesMap.values

    val builtFunctionNClasses get() = builtClassesMap.values.mapNotNull {
        with(it.descriptor as FunctionClassDescriptor) {
            if (functionKind == FunctionClassKind.Function)
                FunctionalInterface(it, arity)
            else null
        }
    }

    private fun createTypeParameter(descriptor: TypeParameterDescriptor): IrTypeParameter =
            symbolTable?.declareGlobalTypeParameter(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, DECLARATION_ORIGIN_FUNCTION_CLASS,
                    descriptor
            )
                    ?: IrTypeParameterImpl(
                            SYNTHETIC_OFFSET,
                            SYNTHETIC_OFFSET,
                            DECLARATION_ORIGIN_FUNCTION_CLASS,
                            IrTypeParameterSymbolImpl(descriptor),
                            descriptor.name,
                            descriptor.index,
                            descriptor.isReified,
                            descriptor.variance
                    )

    private fun createSimpleFunction(
        descriptor: FunctionDescriptor,
        origin: IrDeclarationOrigin,
        returnType: IrType
    ): IrSimpleFunction {
        val functionFactory: (IrSimpleFunctionSymbol) -> IrSimpleFunction = {
            with(descriptor) {
                IrFunctionImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, origin, it, name, visibility, modality, returnType,
                    isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect
                )
            }
        }
        return symbolTable?.declareSimpleFunction(descriptor, functionFactory)
            ?: functionFactory(IrSimpleFunctionSymbolImpl(descriptor))
    }

    private fun createIrClass(symbol: IrClassSymbol, descriptor: ClassDescriptor): IrClass =
        IrFactoryImpl.createIrClassFromDescriptor(offset, offset, DECLARATION_ORIGIN_FUNCTION_CLASS, symbol, descriptor)

    private fun createClass(descriptor: FunctionClassDescriptor, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
        symbolTable?.declarator { createIrClass(it, descriptor) }
            ?: createIrClass(IrClassSymbolImpl(descriptor), descriptor)

    private fun buildClass(descriptor: FunctionClassDescriptor, declarator: SymbolTable.((IrClassSymbol) -> IrClass) -> IrClass): IrClass =
            builtClassesMap.getOrPut(descriptor) {
                createClass(descriptor, declarator).apply {
                    val functionClass = this
                    typeParameters += descriptor.declaredTypeParameters.map { typeParameterDescriptor ->
                        createTypeParameter(typeParameterDescriptor).also {
                            it.parent = this
                            it.superTypes += irBuiltIns.anyNType
                        }
                    }

                    val descriptorToIrParametersMap = typeParameters.map { it.descriptor to it }.toMap()
                    superTypes += descriptor.typeConstructor.supertypes.map { superType ->
                        val arguments = superType.arguments.map { argument ->
                            val argumentClassifierDescriptor = argument.type.constructor.declarationDescriptor
                            val argumentClassifierSymbol = argumentClassifierDescriptor?.let { descriptorToIrParametersMap[it] }
                                    ?: error("Unexpected super type argument: $argumentClassifierDescriptor")
                            makeTypeProjection(argumentClassifierSymbol.defaultType, argument.projectionKind)
                        }
                        val superTypeSymbol = when (val superTypeDescriptor = superType.constructor.declarationDescriptor) {
                            is FunctionClassDescriptor -> buildClass(superTypeDescriptor) {
                                declareClass(superTypeDescriptor) {
                                    createIrClass(it, superTypeDescriptor)
                                }
                            }.symbol
                            functionSymbol.descriptor -> functionSymbol
                            kFunctionSymbol.descriptor -> kFunctionSymbol
                            else -> error("Unexpected super type: $superTypeDescriptor")
                        }
                        IrSimpleTypeImpl(superTypeSymbol, superType.isMarkedNullable, arguments, emptyList())
                    }

                    createParameterDeclarations()

                    val invokeFunctionDescriptor = descriptor.unsubstitutedMemberScope.getContributedFunctions(
                            OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND).single()
                    val isFakeOverride = invokeFunctionDescriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                    if (!isFakeOverride) {
                        val invokeFunctionOrigin =
                                if (isFakeOverride)
                                    IrDeclarationOrigin.FAKE_OVERRIDE
                                else
                                    DECLARATION_ORIGIN_FUNCTION_CLASS
                        declarations += createSimpleFunction(
                                invokeFunctionDescriptor, invokeFunctionOrigin,
                                typeParameters.last().defaultType
                        ).apply {
                            parent = functionClass
                            valueParameters += invokeFunctionDescriptor.valueParameters.map {
                                IrValueParameterImpl(
                                        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, invokeFunctionOrigin,
                                        IrValueParameterSymbolImpl(it), it.name, it.index,
                                        functionClass.typeParameters[it.index].defaultType, null,
                                        it.isCrossinline, it.isNoinline, false, false
                                ).also { it.parent = this }
                            }
                            if (!isFakeOverride)
                                createDispatchReceiverParameter(invokeFunctionOrigin)
                            else {
                                val overriddenFunction = superTypes
                                        .mapNotNull { it.classOrNull?.owner }
                                        .single { it.descriptor is FunctionClassDescriptor }
                                        .simpleFunctions()
                                        .single { it.name == OperatorNameConventions.INVOKE }
                                overriddenSymbols += overriddenFunction.symbol
                                dispatchReceiverParameter = overriddenFunction.dispatchReceiverParameter?.copyTo(this)
                            }
                        }
                    }
                    // Unfortunately, addFakeOverrides() uses some parents but they are only set after PsiToIr phase.
                    // So we add all the fake overrides only when we're supplied with the module (this is done after PsiToIr).
//                    if (this@BuiltInFictitiousFunctionIrClassFactory.module != null)
                        addFakeOverrides()

                    val packageFragmentDescriptor = descriptor.findPackage()
                    val file = filesMap.getOrPut(packageFragmentDescriptor) {
                        IrFileImpl(NaiveSourceBasedFileEntryImpl("[K][Suspend]Functions"), packageFragmentDescriptor).also {
                            this@BuiltInFictitiousFunctionIrClassFactory.module?.files?.add(it)
                        }
                    }
                    parent = file
                    file.declarations += this
                }
            }

    private fun toIrType(wrapped: KotlinType): IrType {
        val kotlinType = wrapped.unwrap()
        return with(IrSimpleTypeBuilder()) {
            classifier =
                    symbolTable?.referenceClassifier(kotlinType.constructor.declarationDescriptor ?: error("No classifier for type $kotlinType"))
            hasQuestionMark = kotlinType.isMarkedNullable
            arguments = kotlinType.arguments.map {
                if (it.isStarProjection) IrStarProjectionImpl
                else makeTypeProjection(toIrType(it.type), it.projectionKind)
            }
            buildSimpleType()
        }
    }

    private fun IrFunction.createValueParameter(descriptor: ParameterDescriptor): IrValueParameter {
        val varargType = if (descriptor is ValueParameterDescriptor) descriptor.varargElementType else null
        return IrValueParameterImpl(
                offset,
                offset,
                memberOrigin,
                IrValueParameterSymbolImpl(descriptor),
                descriptor.name,
                descriptor.indexOrMinusOne,
                toIrType(descriptor.type),
                varargType?.let { toIrType(it) },
                descriptor.isCrossinline,
                descriptor.isNoinline,
            false,
            false
        ).also {
            it.parent = this
        }
    }

    private fun IrClass.addFakeOverrides() {

        val fakeOverrideDescriptors = descriptor.unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.CALLABLES)
                .filterIsInstance<CallableMemberDescriptor>().filter { it.kind === CallableMemberDescriptor.Kind.FAKE_OVERRIDE }

        fun createFakeOverrideFunction(descriptor: FunctionDescriptor, property: IrPropertySymbol?): IrSimpleFunction {
            val returnType = descriptor.returnType?.let { toIrType(it) } ?: error("No return type for $descriptor")


            val functionDeclare = { s: IrSimpleFunctionSymbol ->
                descriptor.run {
                    IrFunctionImpl(
                            offset, offset, memberOrigin, s, name, visibility, modality, returnType,
                            isInline, isExternal, isTailrec, isSuspend, isOperator, isInfix, isExpect,
                            isFakeOverride = true
                    )
                }
            }

            val newFunction = symbolTable?.declareSimpleFunction(descriptor, functionDeclare)
                    ?: functionDeclare(IrSimpleFunctionSymbolImpl(descriptor))

            newFunction.parent = this
            newFunction.overriddenSymbols = descriptor.overriddenDescriptors.mapNotNull { symbolTable?.referenceSimpleFunction(it.original) }
            newFunction.dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.let { newFunction.createValueParameter(it) }
            newFunction.extensionReceiverParameter = descriptor.extensionReceiverParameter?.let { newFunction.createValueParameter(it) }
            newFunction.valueParameters = descriptor.valueParameters.map { newFunction.createValueParameter(it) }
            newFunction.correspondingPropertySymbol = property

            return newFunction
        }

        fun createFakeOverrideProperty(descriptor: PropertyDescriptor): IrProperty {
            val propertyDeclare = { s: IrPropertySymbol ->
                IrPropertyImpl(
                        startOffset = offset,
                        endOffset = offset,
                        origin = memberOrigin,
                        symbol = s,
                        name = descriptor.name,
                        visibility = descriptor.visibility,
                        modality = descriptor.modality,
                        isVar = descriptor.isVar,
                        isConst = descriptor.isConst,
                        isLateinit = descriptor.isLateInit,
                        isDelegated = descriptor.isDelegated,
                        isExternal = descriptor.isExternal,
                        isExpect = descriptor.isExpect,
                        isFakeOverride = memberOrigin == IrDeclarationOrigin.FAKE_OVERRIDE)
            }
            val property = symbolTable?.declareProperty(offset, offset, memberOrigin, descriptor, propertyFactory = propertyDeclare)
                    ?: propertyDeclare(IrPropertySymbolImpl(descriptor))

            property.parent = this
            property.getter = descriptor.getter?.let { g -> createFakeOverrideFunction(g, property.symbol) }
            property.setter = descriptor.setter?.let { s -> createFakeOverrideFunction(s, property.symbol) }

            return property
        }


        fun createFakeOverride(descriptor: CallableMemberDescriptor): IrDeclaration {
            return when (descriptor) {
                is FunctionDescriptor -> createFakeOverrideFunction(descriptor, null)
                is PropertyDescriptor -> createFakeOverrideProperty(descriptor)
                else -> error("Unexpected member $descriptor")
            }
        }

        declarations += fakeOverrideDescriptors.map { createFakeOverride(it) }
    }
}
