/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.stm.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import org.jetbrains.kotlinx.stm.compiler.*
import org.jetbrains.kotlinx.stm.compiler.STM_FIELD_NAME
import org.jetbrains.kotlinx.stm.compiler.STM_INTERFACE
import org.jetbrains.kotlinx.stm.compiler.STM_PACKAGE
import org.jetbrains.kotlinx.stm.compiler.STM_SEARCHER
import org.omg.CORBA.IRObject

// Is creating synthetic origin is a good idea or not?
object STM_PLUGIN_ORIGIN : IrDeclarationOriginImpl("STM")

val BackendContext.externalSymbols: ReferenceSymbolTable get() = ir.symbols.externalSymbolTable


internal val KotlinType?.toClassDescriptor: ClassDescriptor?
    @JvmName("toClassDescriptor")
    get() = this?.constructor?.declarationDescriptor?.let { descriptor ->
        when (descriptor) {
            is ClassDescriptor -> descriptor
            is TypeParameterDescriptor -> descriptor.representativeUpperBound.toClassDescriptor
            else -> null
        }
    }

interface IrBuilderExtension {
    val compilerContext: IrPluginContext

    val BackendContext.localSymbolTable: SymbolTable

    private fun IrClass.declareSimpleFunctionWithExternalOverrides(descriptor: FunctionDescriptor): IrSimpleFunction {
        return compilerContext.symbolTable.declareSimpleFunction(startOffset, endOffset, STM_PLUGIN_ORIGIN, descriptor)
            .also { f ->
                descriptor.overriddenDescriptors.mapTo(f.overriddenSymbols) {
                    compilerContext.symbolTable.referenceSimpleFunction(it.original)
                }
            }
    }

    fun IrClass.contributeFunction(
        descriptor: FunctionDescriptor,
        bodyGen: IrBlockBodyBuilder.(IrFunction) -> Unit
    ) {
        val f = compilerContext.symbolTable.referenceSimpleFunction(descriptor).owner
        f.body = DeclarationIrBuilder(compilerContext, f.symbol, this.startOffset, this.endOffset).irBlockBody(
            this.startOffset,
            this.endOffset
        ) { bodyGen(f) }
    }

    fun IrClass.initField(
        f: IrField,
        initGen: IrBuilderWithScope.() -> IrExpression
    ) {
        val builder = DeclarationIrBuilder(compilerContext, f.symbol, this.startOffset, this.endOffset)

        f.initializer = builder.irExprBody(builder.initGen())
    }

    fun IrClass.contributeConstructor(
        descriptor: ClassConstructorDescriptor,
        declareNew: Boolean = true,
        overwriteValueParameters: Boolean = false,
        bodyGen: IrBlockBodyBuilder.(IrConstructor) -> Unit
    ) {
        val c = if (declareNew) compilerContext.symbolTable.declareConstructor(
            this.startOffset,
            this.endOffset,
            STM_PLUGIN_ORIGIN,
            descriptor
        ) else compilerContext.symbolTable.referenceConstructor(descriptor).owner
        c.parent = this
        c.returnType = descriptor.returnType.toIrType()
        if (declareNew || overwriteValueParameters) c.createParameterDeclarations(
            receiver = null,
            overwriteValueParameters = overwriteValueParameters,
            copyTypeParameters = false
        )
        c.body = DeclarationIrBuilder(compilerContext, c.symbol, this.startOffset, this.endOffset).irBlockBody(
            this.startOffset,
            this.endOffset
        ) { bodyGen(c) }
        this.addMember(c)
    }

    fun IrBuilderWithScope.irInvoke(
        dispatchReceiver: IrExpression? = null,
        callee: IrFunctionSymbol,
        vararg args: IrExpression,
        typeHint: IrType? = null
    ): IrMemberAccessExpression {
        val returnType = typeHint ?: callee.descriptor.returnType!!.toIrType()
        val call = irCall(callee, type = returnType)
        call.dispatchReceiver = dispatchReceiver
        args.forEachIndexed(call::putValueArgument)
        return call
    }

    fun IrBuilderWithScope.irInvoke(
        dispatchReceiver: IrExpression? = null,
        callee: IrFunctionSymbol,
        typeArguments: List<IrType?>,
        valueArguments: List<IrExpression>,
        returnTypeHint: IrType? = null
    ): IrMemberAccessExpression =
        irInvoke(
            dispatchReceiver,
            callee,
            args = *valueArguments.toTypedArray(),
            typeHint = returnTypeHint
        ).also { call -> typeArguments.forEachIndexed(call::putTypeArgument) }

    fun IrBuilderWithScope.createArrayOfExpression(
        arrayElementType: IrType,
        arrayElements: List<IrExpression>
    ): IrExpression {

        val arrayType = compilerContext.symbols.array.typeWith(arrayElementType)
        val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, arrayElementType, arrayElements)
        val typeArguments = listOf(arrayElementType)

        return irCall(compilerContext.symbols.arrayOf, arrayType, typeArguments = typeArguments).apply {
            putValueArgument(0, arg0)
        }
    }

    fun IrBuilderWithScope.irBinOp(name: Name, lhs: IrExpression, rhs: IrExpression): IrExpression {
        val symbol = compilerContext.symbols.getBinaryOperator(
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
            compilerContext.symbolTable.referenceClass(classDescriptor)
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
            compilerContext.symbolTable.withScope(irDeclaration.descriptor) {
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

    fun KotlinType.toIrType() = compilerContext.typeTranslator.translateType(this)

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
                compilerContext.symbolTable.referenceConstructor(anyConstructor)
            )
        }
    }

    fun generateSimplePropertyWithBackingField(
        ownerSymbol: IrValueSymbol,
        propertyDescriptor: PropertyDescriptor,
        propertyParent: IrClass
    ): IrProperty {
        val irProperty = IrPropertyImpl(
            propertyParent.startOffset, propertyParent.endOffset,
            STM_PLUGIN_ORIGIN, false,
            propertyDescriptor
        )
        irProperty.parent = propertyParent
        irProperty.backingField = generatePropertyBackingField(propertyDescriptor, irProperty).apply {
            parent = propertyParent
            correspondingPropertySymbol = irProperty.symbol
        }
        val fieldSymbol = irProperty.backingField!!.symbol
        irProperty.getter = propertyDescriptor.getter?.let { generatePropertyAccessor(it, fieldSymbol) }
            ?.apply { parent = propertyParent }
        irProperty.setter = propertyDescriptor.setter?.let { generatePropertyAccessor(it, fieldSymbol) }
            ?.apply { parent = propertyParent }
        return irProperty
    }

    private fun generatePropertyBackingField(
        propertyDescriptor: PropertyDescriptor,
        originProperty: IrProperty
    ): IrField {
        return compilerContext.symbolTable.declareField(
            originProperty.startOffset,
            originProperty.endOffset,
            STM_PLUGIN_ORIGIN,
            propertyDescriptor,
            propertyDescriptor.type.toIrType()
        )
    }

    fun generatePropertyAccessor(
        descriptor: PropertyAccessorDescriptor,
        fieldSymbol: IrFieldSymbol
    ): IrSimpleFunction {
        val declaration = compilerContext.symbolTable.declareSimpleFunctionWithOverrides(
            fieldSymbol.owner.startOffset,
            fieldSymbol.owner.endOffset,
            STM_PLUGIN_ORIGIN, descriptor
        )
        return declaration.buildWithScope { irAccessor ->
            irAccessor.createParameterDeclarations(receiver = null)
            irAccessor.returnType = irAccessor.descriptor.returnType!!.toIrType()
            irAccessor.body = when (descriptor) {
                is PropertyGetterDescriptor -> generateDefaultGetterBody(descriptor, irAccessor)
                is PropertySetterDescriptor -> generateDefaultSetterBody(descriptor, irAccessor)
                else -> throw AssertionError("Should be getter or setter: $descriptor")
            }
        }
    }

    private fun generateDefaultGetterBody(
        getter: PropertyGetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = getter.correspondingProperty

        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = IrBlockBodyImpl(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(irAccessor.dispatchReceiverParameter!!.symbol, property)

        irBody.statements.add(
            IrReturnImpl(
                startOffset, endOffset, compilerContext.irBuiltIns.nothingType,
                irAccessor.symbol,
                IrGetFieldImpl(
                    startOffset, endOffset,
                    compilerContext.symbolTable.referenceField(property),
                    property.type.toIrType(),
                    receiver
                )
            )
        )
        return irBody
    }

    private fun generateDefaultSetterBody(
        setter: PropertySetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = setter.correspondingProperty

        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = IrBlockBodyImpl(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(irAccessor.dispatchReceiverParameter!!.symbol, property)

        val irValueParameter = irAccessor.valueParameters.single()
        irBody.statements.add(
            IrSetFieldImpl(
                startOffset, endOffset,
                compilerContext.symbolTable.referenceField(property),
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
                    ownerSymbol.owner.startOffset, ownerSymbol.owner.endOffset,
                    ownerSymbol
                )
            else -> throw AssertionError("Property must be in class")
        }
    }

    fun IrFunction.createParameterDeclarations(
        receiver: IrValueParameter?,
        overwriteValueParameters: Boolean = false,
        copyTypeParameters: Boolean = true
    ) {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            this@createParameterDeclarations.startOffset, this@createParameterDeclarations.endOffset,
            STM_PLUGIN_ORIGIN,
            this,
            type.toIrType(),
            null
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        if (!overwriteValueParameters)
            assert(valueParameters.isEmpty())
        else
            valueParameters.clear()
        valueParameters.addAll(descriptor.valueParameters.map { it.irValueParameter() })

        assert(typeParameters.isEmpty())
        if (copyTypeParameters) copyTypeParamsFromDescriptor()
    }

    fun IrFunction.copyTypeParamsFromDescriptor() {
        descriptor.typeParameters.mapTo(typeParameters) {
            IrTypeParameterImpl(
                startOffset, endOffset,
                STM_PLUGIN_ORIGIN,
                it
            ).also { typeParameter ->
                typeParameter.parent = this
            }
        }
    }

    fun kClassTypeFor(projection: TypeProjection): SimpleType {
        val kClass = compilerContext.builtIns.kClass
        return KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, kClass, listOf(projection))
    }

    fun createClassReference(classType: KotlinType, startOffset: Int, endOffset: Int): IrClassReference {
        val clazz = classType.toClassDescriptor!!
        val returnType =
            kClassTypeFor(TypeProjectionImpl(Variance.INVARIANT, classType))
        return IrClassReferenceImpl(
            startOffset,
            endOffset,
            returnType.toIrType(),
            compilerContext.symbolTable.referenceClassifier(clazz),
            classType.toIrType()
        )
    }

    fun IrBuilderWithScope.classReference(classType: KotlinType): IrClassReference = createClassReference(classType, startOffset, endOffset)

    fun findEnumValuesMethod(enumClass: ClassDescriptor): IrFunction {
        assert(enumClass.kind == ClassKind.ENUM_CLASS)
        return compilerContext.symbolTable.referenceClass(enumClass).owner.functions
            .find { it.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER && it.name == Name.identifier("values") }
            ?: throw AssertionError("Enum class does not have .values() function")
    }

    private fun getEnumMembersNames(enumClass: ClassDescriptor): Sequence<String> {
        assert(enumClass.kind == ClassKind.ENUM_CLASS)
        return enumClass.unsubstitutedMemberScope.getContributedDescriptors().asSequence()
            .filterIsInstance<ClassDescriptor>()
            .filter { it.kind == ClassKind.ENUM_ENTRY }
            .map { it.name.toString() }
    }
}

internal fun BackendContext.createTypeTranslator(moduleDescriptor: ModuleDescriptor): TypeTranslator =
    TypeTranslator(externalSymbols, irBuiltIns.languageVersionSettings, moduleDescriptor.builtIns).apply {
        constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable = externalSymbols)
        constantValueGenerator.typeTranslator = this
    }

class STMenerator(val descr: ClassDescriptor, override val compilerContext: IrPluginContext) : IrBuilderExtension {
    private val _table = SymbolTable()
    override val BackendContext.localSymbolTable: SymbolTable
        get() = _table


    fun generateSTMField(irClass: IrClass, field: IrField, initMethod: IrFunctionSymbol, stmSearcherClass: ClassDescriptor) =
        irClass.initField(field) {
            val obj = irGetObject(stmSearcherClass)
            irCallOp(initMethod, field.type, obj)
        }

    fun wrapFunctionIntoTransaction(irClass: IrClass, irFunction: IrSimpleFunction, stmField: IrField, runAtomically: IrFunctionSymbol) {
        val descriptor = irFunction.descriptor

//        val delegateDescriptor = SimpleFunctionDescriptorImpl.create(
//            descriptor.containingDeclaration,
//            descriptor.annotations,
//            Name.identifier("${descriptor.name}${SHARABLE_NAME_SUFFIX}"),
//            descriptor.kind,
//            descriptor.source
//        )
//
//        delegateDescriptor.initialize(
//            descriptor.extensionReceiverParameter,
//            descriptor.dispatchReceiverParameter,
//            descriptor.typeParameters,
//            descriptor.valueParameters,
//            descriptor.returnType,
//            descriptor.modality,
//            Visibilities.PRIVATE
//        )

//        irClass.contributeFunction(delegateDescriptor, declareNew = true) {
//            function.body
//            +irReturn()
//        }

        irClass.contributeFunction(descriptor) {
            val lambdaDescriptor = AnonymousFunctionDescriptor(
                /*containingDeclaration = */ descriptor,
                /*annotations = */ Annotations.EMPTY,
                /*kind = */ CallableMemberDescriptor.Kind.DECLARATION,
                /*source = */ descriptor.source,
                /*isSuspend = */ descriptor.isSuspend
            )

            val lambdaParamDesc = WrappedValueParameterDescriptor()
            val lambdaParamSymbol = IrValueParameterSymbolImpl(lambdaParamDesc)
            val lambdaParam = IrValueParameterImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.DEFINED,
                symbol = lambdaParamSymbol,
                name = Name.identifier("p"),
                index = 0,
                type = context.irBuiltIns.longType,
                varargElementType = null,
                isCrossinline = false,
                isNoinline = false
            )
            lambdaParamDesc.bind(lambdaParam)

            lambdaDescriptor.initialize(
                descriptor.extensionReceiverParameter,
                descriptor.dispatchReceiverParameter,
                listOf(),
                listOf(lambdaParamDesc),
                descriptor.returnType,
                descriptor.modality,
                Visibilities.DEFAULT_VISIBILITY
            )

            val irLambda = compilerContext.symbolTable.declareSimpleFunction(startOffset, endOffset, STM_PLUGIN_ORIGIN, lambdaDescriptor)
            irLambda.body = irFunction.body

            irLambda.valueParameters += lambdaParam
            lambdaParam.parent = irLambda

            irLambda.returnType = irFunction.returnType

            irLambda.patchDeclarationParents(irFunction)

            val lambdaType = runAtomically.descriptor.valueParameters[1].type.toIrType()

            val lambdaExpression = IrFunctionExpressionImpl(
                irLambda.startOffset, irLambda.endOffset,
                lambdaType,
                irLambda,
                IrStatementOrigin.LAMBDA
            ) as IrExpression

            val stmFieldExpr = irGetField(irGet(irFunction.dispatchReceiverParameter!!), stmField)

            +irReturn(
                irInvoke(
                    dispatchReceiver = stmFieldExpr,
                    callee = runAtomically,
                    args = *arrayOf(irNull(), lambdaExpression),
                    typeHint = irFunction.returnType
                )
            )
        }
    }

}

private fun ClassDescriptor.checkPublishMethodResult(type: KotlinType): Boolean =
    KotlinBuiltIns.isInt(type)

private fun ClassDescriptor.checkPublishMethodParameters(parameters: List<ValueParameterDescriptor>): Boolean =
    parameters.size == 0

class StmLoweringException(override val message: String) : Exception()

open class StmIrGenerator {

    companion object {

        private fun findSTMClassDescriptorOrThrow(irClass: IrClass, symbolTable: SymbolTable, className: Name): ClassDescriptor =
            irClass.module.findClassAcrossModuleDependencies(
                ClassId(
                    STM_PACKAGE,
                    className
                )
            ) ?: throw StmLoweringException("Couldn't find $className runtime class in dependencies of module ${irClass.module.name}")

        private fun findSTMMethodDescriptorOrThrow(
            irClass: IrClass,
            symbolTable: SymbolTable,
            className: Name,
            methodName: Name
        ): SimpleFunctionDescriptor =
            findSTMClassDescriptorOrThrow(irClass, symbolTable, className).findMethods(methodName).firstOrNull()
                ?: throw StmLoweringException(
                    "Couldn't find $className.$methodName(...) runtime method in dependencies of module ${irClass.module.name}"
                )

        private fun findSTMMethodIrOrThrow(
            irClass: IrClass,
            symbolTable: SymbolTable,
            className: Name,
            methodName: Name
        ): IrFunctionSymbol =
            symbolTable.referenceSimpleFunction(findSTMMethodDescriptorOrThrow(irClass, symbolTable, className, methodName))

        private fun getSTMField(irClass: IrClass, symbolTable: SymbolTable): IrField {
            val stmClassSymbol = findSTMClassDescriptorOrThrow(irClass, symbolTable, STM_INTERFACE)
                .let(symbolTable::referenceClass)

            val stmType = IrSimpleTypeBuilder().run {
                classifier = stmClassSymbol
                hasQuestionMark = false
                buildSimpleType()
            }

            return irClass.addField {
                name = Name.identifier(STM_FIELD_NAME)
                type = stmType
                visibility = Visibilities.PRIVATE
                origin = IrDeclarationOrigin.DELEGATED_MEMBER
                isFinal = true
                isStatic = false
            }
        }

        private fun getSTMSearchMethod(irClass: IrClass, symbolTable: SymbolTable): IrFunctionSymbol =
            findSTMMethodIrOrThrow(irClass, symbolTable, STM_SEARCHER, SEARCH_STM_METHOD)

        private fun getSTMSearchClass(irClass: IrClass, symbolTable: SymbolTable): ClassDescriptor =
            findSTMClassDescriptorOrThrow(irClass, symbolTable, STM_SEARCHER)

        private fun getRunAtomicallyFun(irClass: IrClass, symbolTable: SymbolTable): IrFunctionSymbol =
            findSTMMethodIrOrThrow(irClass, symbolTable, STM_INTERFACE, RUN_ATOMICALLY_METHOD)

        fun generate(
            irClass: IrClass,
            context: IrPluginContext,
            symbolTable: SymbolTable
        ) {
            val generator = STMenerator(irClass.descriptor, context)

            val stmField = getSTMField(irClass, symbolTable)
            val stmSearch = getSTMSearchMethod(irClass, symbolTable)
            val stmSearchClass = getSTMSearchClass(irClass, symbolTable)
            generator.generateSTMField(irClass, stmField, stmSearch, stmSearchClass)


            val runAtomically = getRunAtomicallyFun(irClass, symbolTable)
            irClass.functions.forEach { f ->
                generator.wrapFunctionIntoTransaction(irClass, f, stmField, runAtomically)
            }
        }
    }
}
