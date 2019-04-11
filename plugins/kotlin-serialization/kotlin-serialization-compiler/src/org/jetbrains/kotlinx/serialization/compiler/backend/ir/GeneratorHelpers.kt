/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializerOrContext
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.contextSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.polymorphicSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

val BackendContext.externalSymbols: ReferenceSymbolTable get() = ir.symbols.externalSymbolTable

internal fun BackendContext.createTypeTranslator(moduleDescriptor: ModuleDescriptor): TypeTranslator =
    TypeTranslator(externalSymbols, irBuiltIns.languageVersionSettings, moduleDescriptor.builtIns).apply {
        constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable = externalSymbols)
        constantValueGenerator.typeTranslator = this
    }

interface IrBuilderExtension {
    val compilerContext: BackendContext
    val translator: TypeTranslator

    val BackendContext.localSymbolTable: SymbolTable

    private fun IrClass.declareSimpleFunctionWithExternalOverrides(descriptor: FunctionDescriptor): IrSimpleFunction {
        return compilerContext.localSymbolTable.declareSimpleFunction(startOffset, endOffset, SERIALIZABLE_PLUGIN_ORIGIN, descriptor).also {f ->
            descriptor.overriddenDescriptors.mapTo(f.overriddenSymbols) {
                compilerContext.externalSymbols.referenceSimpleFunction(it.original)
            }
        }
    }

    fun IrClass.contributeFunction(descriptor: FunctionDescriptor, fromStubs: Boolean = false, bodyGen: IrBlockBodyBuilder.(IrFunction) -> Unit) {
        val f: IrSimpleFunction = if (!fromStubs) declareSimpleFunctionWithExternalOverrides(
            descriptor
        ) else compilerContext.externalSymbols.referenceSimpleFunction(descriptor).owner
        f.parent = this
        f.returnType = descriptor.returnType!!.toIrType()
        if (!fromStubs) f.createParameterDeclarations(this.thisReceiver!!)
        f.body = compilerContext.createIrBuilder(f.symbol).at(this).irBlockBody(this.startOffset, this.endOffset) { bodyGen(f) }
        this.addMember(f)
    }

    fun IrClass.contributeConstructor(
        descriptor: ClassConstructorDescriptor,
        fromStubs: Boolean = false,
        bodyGen: IrBlockBodyBuilder.(IrConstructor) -> Unit
    ) {
        val c = if (!fromStubs) compilerContext.localSymbolTable.declareConstructor(
            this.startOffset,
            this.endOffset,
            SERIALIZABLE_PLUGIN_ORIGIN,
            descriptor
        ) else compilerContext.externalSymbols.referenceConstructor(descriptor).owner
        c.parent = this
        c.returnType = descriptor.returnType.toIrType()
        if (!fromStubs) c.createParameterDeclarations(receiver = null)
        if (c.typeParameters.isEmpty()) {
            c.copyTypeParamsFromDescriptor()
        }
        c.body = compilerContext.createIrBuilder(c.symbol).at(this).irBlockBody(this.startOffset, this.endOffset) { bodyGen(c) }
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

    fun IrBuilderWithScope.irInvoke(
        dispatchReceiver: IrExpression? = null,
        callee: IrFunctionSymbol,
        typeArguments: List<IrType?>,
        valueArguments: List<IrExpression>,
        returnTypeHint: IrType? = null
    ): IrCall = irInvoke(
        dispatchReceiver,
        callee,
        args = *valueArguments.toTypedArray(),
        typeHint = returnTypeHint
    ).also { call -> typeArguments.forEachIndexed(call::putTypeArgument) }

    fun IrBuilderWithScope.createArrayOfExpression(
            arrayElementType: IrType,
            arrayElements: List<IrExpression>
    ): IrExpression {

        val arrayType = compilerContext.ir.symbols.array.typeWith(arrayElementType)
        val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, arrayElementType, arrayElements)
        val typeArguments = listOf(arrayElementType)

        return irCall(compilerContext.ir.symbols.arrayOf, arrayType, typeArguments = typeArguments).apply {
            putValueArgument(0, arg0)
        }
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
            propertyParent.startOffset, propertyParent.endOffset,
            SERIALIZABLE_PLUGIN_ORIGIN, false,
            propertyDescriptor
        )
        irProperty.parent = propertyParent
        irProperty.backingField =
            generatePropertyBackingField(propertyDescriptor, irProperty).apply { parent = propertyParent }
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
        return compilerContext.localSymbolTable.declareField(
            originProperty.startOffset,
            originProperty.endOffset,
            SERIALIZABLE_PLUGIN_ORIGIN,
            propertyDescriptor,
            propertyDescriptor.type.toIrType()
        )
    }

    fun generatePropertyAccessor(
        descriptor: PropertyAccessorDescriptor,
        fieldSymbol: IrFieldSymbol
    ): IrSimpleFunction {
        // Declaration can also be called from user code. Since we lookup descriptor getter in externalSymbols
        // (see generateSave/generateLoad), seems it is correct approach to declare getter lazily there.
        val declaration = compilerContext.externalSymbols.referenceSimpleFunction(descriptor).owner
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
                    ownerSymbol.owner.startOffset, ownerSymbol.owner.endOffset,
//                symbolTable.referenceValue(containingDeclaration.thisAsReceiverParameter)
                    ownerSymbol
                )
            else -> throw AssertionError("Property must be in class")
        }
    }

    fun IrFunction.createParameterDeclarations(receiver: IrValueParameter?) {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            this@createParameterDeclarations.startOffset, this@createParameterDeclarations.endOffset,
            SERIALIZABLE_PLUGIN_ORIGIN,
            this,
            type.toIrType(),
            null
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        assert(valueParameters.isEmpty())
        descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }

        assert(typeParameters.isEmpty())
        copyTypeParamsFromDescriptor()
    }

    fun IrFunction.copyTypeParamsFromDescriptor() {
        descriptor.typeParameters.mapTo(typeParameters) {
            IrTypeParameterImpl(
                startOffset, endOffset,
                SERIALIZABLE_PLUGIN_ORIGIN,
                it
            ).also { typeParameter ->
                typeParameter.parent = this
            }
        }
    }

    fun IrBuilderWithScope.classReference(classType: KotlinType): IrClassReference {
        val clazz = classType.toClassDescriptor!!
        val kClass = clazz.module.findClassAcrossModuleDependencies(ClassId(FqName("kotlin.reflect"), Name.identifier("KClass")))!!
        val returnType = KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, kClass, listOf(TypeProjectionImpl(Variance.INVARIANT, classType)))
        return IrClassReferenceImpl(
            startOffset,
            endOffset,
            returnType.toIrType(),
            compilerContext.externalSymbols.referenceClassifier(clazz),
            classType.toIrType()
        )
    }

    fun buildInitializersRemapping(irClass: IrClass): (IrField) -> IrExpression? {
        val original = irClass.constructors.singleOrNull { it.isPrimary }
            ?: throw IllegalStateException("Serializable class must have single primary constructor")
        // default arguments of original constructor
        val defaultsMap: Map<ParameterDescriptor, IrExpression?> =
            original.valueParameters.associate { it.descriptor to it.defaultValue?.expression }
        return fun(f: IrField): IrExpression? {
            val i = f.initializer?.expression ?: return null
            return if (i is IrGetValueImpl && i.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER) {
                // this is a primary constructor property, use corresponding default of value parameter
                defaultsMap.getValue(i.descriptor as ParameterDescriptor)
            } else {
                i
            }
        }
    }

    fun findEnumValuesMethod(enumClass: ClassDescriptor): IrFunction {
        assert(enumClass.kind == ClassKind.ENUM_CLASS)
        return compilerContext.externalSymbols.referenceClass(enumClass).owner.functions
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

    // Does not use sti and therefore does not perform encoder calls optimization
    fun IrBuilderWithScope.serializerTower(generator: SerializerIrGenerator, dispatchReceiverParameter: IrValueParameter, property: SerializableProperty): IrExpression? {
        val nullableSerClass =
                compilerContext.externalSymbols.referenceClass(property.module.getClassFromInternalSerializationPackage(SpecialBuiltins.nullableSerializer))
        val serializer =
                property.serializableWith?.toClassDescriptor
                        ?: if (!property.type.isTypeParameter()) generator.findTypeSerializerOrContext(
                                property.module,
                                property.type,
                                property.descriptor.annotations,
                                property.descriptor.findPsi()
                        ) else null
        return serializerInstance(generator, dispatchReceiverParameter, generator.serializableDescriptor, serializer, property.module, property.type, genericIndex = property.genericIndex)
                ?.let { expr -> wrapWithNullableSerializerIfNeeded(property.module, property.type, expr, nullableSerClass) }
    }

    private fun IrBuilderWithScope.wrapWithNullableSerializerIfNeeded(module: ModuleDescriptor, type: KotlinType, expression: IrExpression, nullableSerializerClass: IrClassSymbol): IrExpression {
        return if (type.isMarkedNullable)
            irInvoke(null, nullableSerializerClass.constructors.toList()[0],
                typeArguments  = listOf(type.makeNotNullable().toIrType()),
                valueArguments = listOf(expression),
                returnTypeHint = wrapIrTypeIntoKSerializerIrType(module, type.toIrType())
            )
        else
            expression
    }


    fun wrapIrTypeIntoKSerializerIrType(module: ModuleDescriptor, type: IrType): IrType {
        val kSerClass =
            compilerContext.externalSymbols.referenceClass(module.getClassFromSerializationPackage(SerialEntityNames.KSERIALIZER_CLASS))
        return IrSimpleTypeImpl(
            kSerClass, hasQuestionMark = false, arguments = listOf(
                makeTypeProjection(type, Variance.INVARIANT)
            ), annotations = emptyList()
        )
    }

    fun IrBuilderWithScope.serializerInstance(
        enclosingGenerator: SerializerIrGenerator,
        dispatchReceiverParameter: IrValueParameter,
        serializableDescriptor: ClassDescriptor,
        serializerClassOriginal: ClassDescriptor?,
        module: ModuleDescriptor,
        kType: KotlinType,
        genericIndex: Int? = null
    ): IrExpression? {
        val nullableSerClass =
            compilerContext.externalSymbols.referenceClass(module.getClassFromInternalSerializationPackage(SpecialBuiltins.nullableSerializer))
        if (serializerClassOriginal == null) {
            if (genericIndex == null) return null
            val thiz = enclosingGenerator.irClass.thisReceiver!!
            val prop = enclosingGenerator.localSerializersFieldsDescriptors[genericIndex]
            return irGetField(irGet(dispatchReceiverParameter), compilerContext.localSymbolTable.referenceField(prop).owner)
        }
        if (serializerClassOriginal.kind == ClassKind.OBJECT) {
            return irGetObject(serializerClassOriginal)
        } else {
            var serializerClass = serializerClassOriginal
            var args: List<IrExpression>
            var typeArgs: List<IrType?>
            when (serializerClassOriginal.classId) {
                contextSerializerId, polymorphicSerializerId -> {
                    args = listOf(classReference(kType))
                    typeArgs = listOf(kType.toIrType())
                }
                enumSerializerId -> {
                    serializerClass = serializableDescriptor.getClassFromInternalSerializationPackage("CommonEnumSerializer")
                    args = kType.toClassDescriptor!!.let { enumDesc ->
                        listOf(
                            irString(enumDesc.name.toString()),
                            irCall(findEnumValuesMethod(enumDesc)),
                            createArrayOfExpression(
                                compilerContext.irBuiltIns.stringType,
                                getEnumMembersNames(enumDesc).map { irString(it) }.toList()
                            )
                        )
                    }
                    typeArgs = listOf(kType.toIrType())
                }
                else -> {
                    args = kType.arguments.map {
                        val argSer = enclosingGenerator.findTypeSerializerOrContext(module, it.type, sourceElement = serializerClassOriginal.findPsi())
                        val expr = serializerInstance(enclosingGenerator, dispatchReceiverParameter, serializableDescriptor, argSer, module, it.type, it.type.genericIndex)
                            ?: return null
                        wrapWithNullableSerializerIfNeeded(module, it.type, expr, nullableSerClass)
                    }
                    typeArgs = kType.arguments.map { it.type.toIrType() }
                }

            }
            if (serializerClassOriginal.classId == referenceArraySerializerId) {
                args = listOf(classReference(kType.arguments[0].type)) + args
                typeArgs = listOf(typeArgs[0].makeNotNull()) + typeArgs
            }


            val serializable = getSerializableClassDescriptorBySerializer(serializerClass)
            val ctor = if (serializable?.declaredTypeParameters?.isNotEmpty() == true) {
                requireNotNull(
                    KSerializerDescriptorResolver.findSerializerConstructorForTypeArgumentsSerializers(serializerClass)
                ) { "Generated serializer does not have constructor with required number of arguments" }
                    .let { compilerContext.externalSymbols.referenceConstructor(it) }
            } else {
                compilerContext.externalSymbols.referenceConstructor(serializerClass.unsubstitutedPrimaryConstructor!!)
            }
            val propertyTypeHint = kType.toIrType()
            val returnType = wrapIrTypeIntoKSerializerIrType(module, propertyTypeHint)
            return irInvoke(null, ctor, typeArguments = typeArgs, valueArguments = args, returnTypeHint = returnType)
        }
    }

    fun IrClass.serializableSyntheticConstructor(): IrConstructorSymbol =
        this.constructors.single { it.origin == SERIALIZABLE_PLUGIN_ORIGIN }.symbol
}
