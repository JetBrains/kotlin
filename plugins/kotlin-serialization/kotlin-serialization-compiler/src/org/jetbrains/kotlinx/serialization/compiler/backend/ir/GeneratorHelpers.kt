/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import org.jetbrains.kotlinx.serialization.compiler.backend.common.AbstractSerialGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.common.allSealedSerializableSubclassesFor
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializerOrContext
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.*
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

interface IrBuilderExtension {
    val compilerContext: SerializationPluginContext

    fun IrClass.contributeFunction(descriptor: FunctionDescriptor, bodyGen: IrBlockBodyBuilder.(IrFunction) -> Unit) {
        val functionSymbol = compilerContext.symbolTable.referenceSimpleFunction(descriptor)
        assert(functionSymbol.isBound)
        val f: IrSimpleFunction = functionSymbol.owner
        // TODO: default parameters
        f.body = DeclarationIrBuilder(compilerContext, f.symbol, this.startOffset, this.endOffset).irBlockBody(
            this.startOffset,
            this.endOffset
        ) { bodyGen(f) }
    }

    fun IrClass.contributeConstructor(
        descriptor: ClassConstructorDescriptor,
        declareNew: Boolean = true,
        overwriteValueParameters: Boolean = false,
        bodyGen: IrBlockBodyBuilder.(IrConstructor) -> Unit
    ) {
        val ctorSymbol = compilerContext.symbolTable.referenceConstructor(descriptor)
        assert(ctorSymbol.isBound)

        val c = ctorSymbol.owner

        c.body = DeclarationIrBuilder(compilerContext, c.symbol, this.startOffset, this.endOffset).irBlockBody(
            this.startOffset,
            this.endOffset
        ) { bodyGen(c) }
    }

    fun IrBuilderWithScope.irInvoke(
        dispatchReceiver: IrExpression? = null,
        callee: IrFunctionSymbol,
        vararg args: IrExpression,
        typeHint: IrType? = null
    ): IrMemberAccessExpression {
        assert(callee.isBound) { "Symbol $callee expected to be bound" }
        val returnType = typeHint ?: callee.run { owner.returnType }
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

        val arrayType = compilerContext.irBuiltIns.arrayClass.typeWith(arrayElementType)
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
            compilerContext.symbolTable.withReferenceScope(irDeclaration) {
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

    // note: this method should be used only for properties from current module. Fields from other modules are private and inaccessible.
    val SerializableProperty.irField: IrField get() = compilerContext.symbolTable.referenceField(this.descriptor).owner

    val SerializableProperty.irProp: IrProperty
        get() {
            val desc = this.descriptor
            // this API is used to reference both current module descriptors and external ones (because serializable class can be in any of them),
            // so we use descriptor api for current module because it is not possible to obtain FQname for e.g. local classes.
            return if (desc.module == compilerContext.moduleDescriptor) {
                compilerContext.symbolTable.referenceProperty(desc).owner
            } else {
                compilerContext.referenceProperties(this.descriptor.fqNameSafe).single().owner
            }
        }

    fun IrBuilderWithScope.getProperty(receiver: IrExpression, property: IrProperty): IrExpression {
        return if (property.getter != null)
            irGet(property.getter!!.returnType, receiver, property.getter!!.symbol)
        else
            irGetField(receiver, property.backingField!!)
    }

    fun IrBuilderWithScope.setProperty(receiver: IrExpression, property: IrProperty, value: IrExpression): IrExpression {
        return if (property.setter != null)
            irSet(property.setter!!.returnType, receiver, property.setter!!.symbol, value)
        else
            irSetField(receiver, property.backingField!!, value)
    }

    /*
     The rest of the file is mainly copied from FunctionGenerator.
     However, I can't use it's directly because all generateSomething methods require KtProperty (psi element)
     Also, FunctionGenerator itself has DeclarationGenerator as ctor param, which is a part of psi2ir
     (it can be instantiated here, but I don't know how good is that idea)
     */

    fun IrBuilderWithScope.generateAnySuperConstructorCall(toBuilder: IrBlockBodyBuilder) {
        val anyConstructor = compilerContext.irBuiltIns.anyClass.owner.declarations.single { it is IrConstructor } as IrConstructor
        with(toBuilder) {
            +IrDelegatingConstructorCallImpl(
                startOffset, endOffset,
                compilerContext.irBuiltIns.unitType,
                anyConstructor.symbol
            )
        }
    }

    fun generateSimplePropertyWithBackingField(
        ownerSymbol: IrValueSymbol,
        propertyDescriptor: PropertyDescriptor,
        propertyParent: IrClass,
        declare: Boolean
    ): IrProperty {
        val irPropertySymbol = compilerContext.symbolTable.referenceProperty(propertyDescriptor)
        assert(irPropertySymbol.isBound || declare)

        if (declare) {
            IrPropertyImpl(propertyParent.startOffset, propertyParent.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, propertyDescriptor, irPropertySymbol).also {
                it.parent = propertyParent
                propertyParent.addMember(it)
            }
        }
        val irProperty = irPropertySymbol.owner

        irProperty.backingField = generatePropertyBackingField(propertyDescriptor, irProperty).apply {
            parent = propertyParent
            correspondingPropertySymbol = irPropertySymbol
        }
        val fieldSymbol = irProperty.backingField!!.symbol
        irProperty.getter = propertyDescriptor.getter?.let { generatePropertyAccessor(it, fieldSymbol, declare) }
            ?.apply { parent = propertyParent }
        irProperty.setter = propertyDescriptor.setter?.let { generatePropertyAccessor(it, fieldSymbol, declare) }
            ?.apply { parent = propertyParent }
        return irProperty
    }

    private fun generatePropertyBackingField(
        propertyDescriptor: PropertyDescriptor,
        originProperty: IrProperty
    ): IrField {
        val fieldSymbol = compilerContext.symbolTable.referenceField(propertyDescriptor)

        if (fieldSymbol.isBound) return fieldSymbol.owner

        return originProperty.run {
            // TODO: type parameters
            IrFieldImpl(startOffset, endOffset, SERIALIZABLE_PLUGIN_ORIGIN, propertyDescriptor, propertyDescriptor.type.toIrType(), symbol = fieldSymbol)
        }
    }

    fun generatePropertyAccessor(
        descriptor: PropertyAccessorDescriptor,
        fieldSymbol: IrFieldSymbol,
        declare: Boolean
    ): IrSimpleFunction {
        val symbol = compilerContext.symbolTable.referenceSimpleFunction(descriptor)
        assert(symbol.isBound || declare)

        if (declare) {
            IrFunctionImpl(
                fieldSymbol.owner.startOffset,
                fieldSymbol.owner.endOffset,
                SERIALIZABLE_PLUGIN_ORIGIN,
                symbol,
                descriptor.returnType!!.toIrType(),
                descriptor
            ).also { f ->
                generateOverriddenFunctionSymbols(f, compilerContext.symbolTable)
                f.createParameterDeclarations(receiver = null)
                f.returnType = descriptor.returnType!!.toIrType()
                f.correspondingPropertySymbol = fieldSymbol.owner.correspondingPropertySymbol
            }
        }

        val irAccessor = symbol.owner
        irAccessor.body = when (descriptor) {
            is PropertyGetterDescriptor -> generateDefaultGetterBody(descriptor, irAccessor)
            is PropertySetterDescriptor -> generateDefaultSetterBody(descriptor, irAccessor)
            else -> throw AssertionError("Should be getter or setter: $descriptor")
        }

        return irAccessor
    }

    private fun generateDefaultGetterBody(
        getter: PropertyGetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = getter.correspondingProperty
        val irProperty = irAccessor.correspondingPropertySymbol?.owner ?: error("Expected property for $getter")

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
                    irProperty.backingField?.symbol ?: error("Property expected to have backing field"),
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
        val irProperty = irAccessor.correspondingPropertySymbol?.owner ?: error("Expected corresponding property for accessor $setter")
        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = IrBlockBodyImpl(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(irAccessor.dispatchReceiverParameter!!.symbol, property)

        val irValueParameter = irAccessor.valueParameters.single()
        irBody.statements.add(
            IrSetFieldImpl(
                startOffset, endOffset,
                irProperty.backingField?.symbol ?: error("Property $property expected to have backing field"),
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
            SERIALIZABLE_PLUGIN_ORIGIN,
            this,
            type.toIrType(),
            (this as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        ).also {
            it.parent = this@createParameterDeclarations
        }

        if (copyTypeParameters) {
            assert(typeParameters.isEmpty())
            copyTypeParamsFromDescriptor()
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        if (!overwriteValueParameters)
            assert(valueParameters.isEmpty())

        valueParameters = descriptor.valueParameters.map { it.irValueParameter() }
    }

    fun IrFunction.copyTypeParamsFromDescriptor() {
        val newTypeParameters = descriptor.typeParameters.map {
            IrTypeParameterImpl(
                startOffset, endOffset,
                SERIALIZABLE_PLUGIN_ORIGIN,
                it
            ).also { typeParameter ->
                typeParameter.parent = this
            }
        }

        newTypeParameters.forEach { typeParameter ->
            typeParameter.superTypes.addAll(typeParameter.descriptor.upperBounds.map { it.toIrType() })
        }

        typeParameters = newTypeParameters
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
            compilerContext.referenceClass(clazz.fqNameSafe) ?: error("Couldn't load class $clazz"),
            classType.toIrType()
        )
    }

    fun IrBuilderWithScope.classReference(classType: KotlinType): IrClassReference = createClassReference(classType, startOffset, endOffset)

    fun buildInitializersRemapping(irClass: IrClass): (IrField) -> IrExpression? {
        val original = irClass.constructors.singleOrNull { it.isPrimary }
            ?: throw IllegalStateException("Serializable class must have single primary constructor")
        // default arguments of original constructor
        val defaultsMap: Map<ParameterDescriptor, IrExpression?> =
            original.valueParameters.associate { it.descriptor to it.defaultValue?.expression }
        return fun(f: IrField): IrExpression? {
            val i = f.initializer?.expression ?: return null
            val irExpression =
                if (i is IrGetValueImpl && i.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER) {
                    // this is a primary constructor property, use corresponding default of value parameter
                    defaultsMap.getValue(i.symbol.descriptor as ParameterDescriptor)
                } else {
                    i
                }
            return irExpression?.deepCopyWithVariables()
        }
    }

    fun findEnumValuesMethod(enumClass: ClassDescriptor): IrFunction {
        assert(enumClass.kind == ClassKind.ENUM_CLASS)
        return compilerContext.referenceClass(enumClass.fqNameSafe)?.let {
            it.owner.functions.find { it.origin == IrDeclarationOrigin.ENUM_CLASS_SPECIAL_MEMBER && it.name == Name.identifier("values") }
                ?: throw AssertionError("Enum class does not have .values() function")
        } ?: error("Couldn't load class $enumClass")
    }

    private fun getEnumMembersNames(enumClass: ClassDescriptor): Sequence<String> {
        assert(enumClass.kind == ClassKind.ENUM_CLASS)
        return enumClass.unsubstitutedMemberScope.getContributedDescriptors().asSequence()
            .filterIsInstance<ClassDescriptor>()
            .filter { it.kind == ClassKind.ENUM_ENTRY }
            .map { it.name.toString() }
    }

    // Does not use sti and therefore does not perform encoder calls optimization
    fun IrBuilderWithScope.serializerTower(
        generator: SerializerIrGenerator,
        dispatchReceiverParameter: IrValueParameter,
        property: SerializableProperty
    ): IrExpression? {
        val nullableSerializerFqn = getInternalPackageFqn(SpecialBuiltins.nullableSerializer)
        val nullableSerClass = compilerContext.referenceClass(nullableSerializerFqn) ?: error("Couldn't find class $nullableSerializerFqn")
        val serializer =
            property.serializableWith?.toClassDescriptor
                ?: if (!property.type.isTypeParameter()) generator.findTypeSerializerOrContext(
                    property.module,
                    property.type,
                    property.descriptor.findPsi()
                ) else null
        return serializerInstance(
            generator,
            dispatchReceiverParameter,
            serializer,
            property.module,
            property.type,
            genericIndex = property.genericIndex
        )
            ?.let { expr -> wrapWithNullableSerializerIfNeeded(property.module, property.type, expr, nullableSerClass) }
    }

    private fun IrBuilderWithScope.wrapWithNullableSerializerIfNeeded(
        module: ModuleDescriptor,
        type: KotlinType,
        expression: IrExpression,
        nullableSerializerClass: IrClassSymbol
    ): IrExpression {
        return if (type.isMarkedNullable) {
            val classDeclaration = nullableSerializerClass.owner
            val nullableConstructor = classDeclaration.declarations.first { it is IrConstructor } as IrConstructor
            val resultType = type.makeNotNullable()
            val typeParameters = classDeclaration.typeParameters
            val typeArguments = listOf(resultType.toIrType())
            irInvoke(
                null, nullableConstructor.symbol,
                typeArguments = typeArguments,
                valueArguments = listOf(expression),
                // Return type should be correctly substituted
                returnTypeHint = nullableConstructor.returnType.substitute(typeParameters, typeArguments)
            )
        } else {
            expression
        }
    }


    fun wrapIrTypeIntoKSerializerIrType(module: ModuleDescriptor, type: IrType, variance: Variance = Variance.INVARIANT): IrType {
        val serializerFqn = getSerializationPackageFqn(SerialEntityNames.KSERIALIZER_CLASS)
        val kSerClass = compilerContext.referenceClass(serializerFqn) ?: error("Couldn't find class $serializerFqn")
        return IrSimpleTypeImpl(
            kSerClass, hasQuestionMark = false, arguments = listOf(
                makeTypeProjection(type, variance)
            ), annotations = emptyList()
        )
    }

    fun IrBuilderWithScope.serializerInstance(
        enclosingGenerator: SerializerIrGenerator,
        dispatchReceiverParameter: IrValueParameter,
        serializerClassOriginal: ClassDescriptor?,
        module: ModuleDescriptor,
        kType: KotlinType,
        genericIndex: Int? = null
    ): IrExpression? = serializerInstance(
        enclosingGenerator,
        serializerClassOriginal,
        module,
        kType,
        genericIndex
    ) { it, _ ->
        val prop = enclosingGenerator.localSerializersFieldsDescriptors[it]
        irGetField(irGet(dispatchReceiverParameter), compilerContext.symbolTable.referenceField(prop).owner)
    }

    fun IrBuilderWithScope.serializerInstance(
        enclosingGenerator: AbstractSerialGenerator,
        serializerClassOriginal: ClassDescriptor?,
        module: ModuleDescriptor,
        kType: KotlinType,
        genericIndex: Int? = null,
        genericGetter: ((Int, KotlinType) -> IrExpression)? = null
    ): IrExpression? {
        val nullableClassFqn = getInternalPackageFqn(SpecialBuiltins.nullableSerializer)
        val nullableSerClass = compilerContext.referenceClass(nullableClassFqn) ?: error("Expecting class $nullableClassFqn")
        if (serializerClassOriginal == null) {
            if (genericIndex == null) return null
            return genericGetter?.invoke(genericIndex, kType)
        }
        if (serializerClassOriginal.kind == ClassKind.OBJECT) {
            return irGetObject(serializerClassOriginal)
        } else {
            fun instantiate(serializer: ClassDescriptor?, type: KotlinType): IrExpression? {
                val expr = serializerInstance(
                    enclosingGenerator,
                    serializer,
                    module,
                    type,
                    type.genericIndex,
                    genericGetter
                ) ?: return null
                return wrapWithNullableSerializerIfNeeded(module, type, expr, nullableSerClass)
            }
            var serializerClass = serializerClassOriginal
            var args: List<IrExpression>
            var typeArgs: List<IrType?>
            val thisIrType = kType.toIrType()
            when (serializerClassOriginal.classId) {
                contextSerializerId, polymorphicSerializerId -> {
                    args = listOf(classReference(kType))
                    typeArgs = listOf(thisIrType)
                }
                objectSerializerId -> {
                    args = listOf(irString(kType.serialName()), irGetObject(kType.toClassDescriptor!!))
                    typeArgs = listOf(thisIrType)
                }
                sealedSerializerId -> {
                    args = mutableListOf<IrExpression>().apply {
                        add(irString(kType.serialName()))
                        add(classReference(kType))
                        val (subclasses, subSerializers) = enclosingGenerator.allSealedSerializableSubclassesFor(
                            kType.toClassDescriptor!!,
                            module
                        )
                        val projectedOutCurrentKClass = kClassTypeFor(TypeProjectionImpl(Variance.OUT_VARIANCE, kType))
                        add(
                            createArrayOfExpression(
                                projectedOutCurrentKClass.toIrType(),
                                subclasses.map { classReference(it) }
                            )
                        )
                        add(
                            createArrayOfExpression(
                                wrapIrTypeIntoKSerializerIrType(module, thisIrType, variance = Variance.OUT_VARIANCE),
                                subSerializers.mapIndexed { i, serializer ->
                                    val type = subclasses[i]
                                    val expr = serializerInstance(
                                        enclosingGenerator,
                                        serializer,
                                        module,
                                        type,
                                        type.genericIndex
                                    ) { _, genericType ->
                                        serializerInstance(
                                            enclosingGenerator,
                                            module.getClassFromSerializationPackage(
                                                SpecialBuiltins.polymorphicSerializer
                                            ),
                                            module,
                                            (genericType.constructor.declarationDescriptor as TypeParameterDescriptor).representativeUpperBound
                                        )!!
                                    }!!
                                    wrapWithNullableSerializerIfNeeded(module, type, expr, nullableSerClass)
                                }
                            )
                        )
                    }
                    typeArgs = listOf(thisIrType)
                }
                enumSerializerId -> {
                    serializerClass = module.getClassFromInternalSerializationPackage(SpecialBuiltins.enumSerializer)
                    args = kType.toClassDescriptor!!.let { enumDesc ->
                        listOf(
                            irString(enumDesc.serialName()),
                            irCall(findEnumValuesMethod(enumDesc))
                        )
                    }
                    typeArgs = listOf(thisIrType)
                }
                else -> {
                    args = kType.arguments.map {
                        val argSer = enclosingGenerator.findTypeSerializerOrContext(
                            module,
                            it.type,
                            sourceElement = serializerClassOriginal.findPsi()
                        )
                        instantiate(argSer, it.type) ?: return null
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
                    findSerializerConstructorForTypeArgumentsSerializers(serializerClass)
                ) { "Generated serializer does not have constructor with required number of arguments" }
            } else {
                compilerContext.referenceConstructors(serializerClass.fqNameSafe).single { it.owner.isPrimary }
            }
            // Return type should be correctly substituted
            assert(ctor.isBound)
            val ctorDecl = ctor.owner
            val typeParameters = ctorDecl.parentAsClass.typeParameters
            val substitutedReturnType = ctorDecl.returnType.substitute(typeParameters, typeArgs)
            return irInvoke(null, ctor, typeArguments = typeArgs, valueArguments = args, returnTypeHint = substitutedReturnType)
        }
    }

    private fun findSerializerConstructorForTypeArgumentsSerializers(serializer: ClassDescriptor): IrConstructorSymbol? {
        val serializableImplementationTypeArguments = extractKSerializerArgumentFromImplementation(serializer)?.arguments
            ?: throw AssertionError("Serializer does not implement KSerializer??")

        val typeParamsCount = serializableImplementationTypeArguments.size
        if (typeParamsCount == 0) return null //don't need it
        val constructors = compilerContext.referenceConstructors(serializer.fqNameSafe)

        fun isKSerializer(type: IrType): Boolean {
            val simpleType = type as? IrSimpleType ?: return false
            val classifier = simpleType.classifier as? IrClassSymbol ?: return false
            return classifier.owner.fqNameWhenAvailable == SerialEntityNames.KSERIALIZER_NAME_FQ
        }

        return constructors.singleOrNull {
            it.owner.valueParameters.let { vps -> vps.size == typeParamsCount && vps.all { vp -> isKSerializer(vp.type) } }
        }
    }

    private fun IrConstructor.isSerializationCtor(): Boolean {
        val serialMarker =
            compilerContext.referenceClass(SerializationPackages.packageFqName.child(SerialEntityNames.SERIAL_CTOR_MARKER_NAME))

        return valueParameters.lastOrNull()?.run {
            name == SerialEntityNames.dummyParamName && type.classifierOrNull == serialMarker
        } == true
    }

    fun serializableSyntheticConstructor(forClass: IrClass): IrConstructorSymbol {
        return forClass.declarations.filterIsInstance<IrConstructor>().single { it.isSerializationCtor() }.symbol
    }

    fun IrClass.getSuperClassOrAny(): IrClass {
        val superClasses = superTypes.mapNotNull { it.classOrNull }.map { it.owner }

        return superClasses.singleOrNull { it.kind == ClassKind.CLASS } ?: compilerContext.irBuiltIns.anyClass.owner
    }

}