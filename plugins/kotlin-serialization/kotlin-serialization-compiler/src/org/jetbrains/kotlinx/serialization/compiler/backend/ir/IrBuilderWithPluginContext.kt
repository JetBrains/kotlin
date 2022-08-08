/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.KSerializerDescriptorResolver
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationDependencies.LAZY_FQ
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationDependencies.LAZY_MODE_FQ
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationDependencies.LAZY_PUBLICATION_MODE_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.hasSerializableOrMetaAnnotation
import org.jetbrains.kotlinx.serialization.compiler.resolve.isInheritableSerialInfoAnnotation
import org.jetbrains.kotlinx.serialization.compiler.resolve.isSerialInfoAnnotation


interface IrBuilderWithPluginContext {
    val compilerContext: SerializationPluginContext

    fun <F: IrFunction> addFunctionBody(function: F, bodyGen: IrBlockBodyBuilder.(F) -> Unit) {
        val parentClass = function.parent
        val startOffset = function.startOffset.takeIf { it >= 0 } ?: parentClass.startOffset
        val endOffset = function.endOffset.takeIf { it >= 0 } ?: parentClass.endOffset
        function.body = DeclarationIrBuilder(compilerContext, function.symbol, startOffset, endOffset).irBlockBody(
            startOffset,
            endOffset
        ) { bodyGen(function) }
    }

    fun IrClass.createLambdaExpression(
        type: IrType,
        bodyGen: IrBlockBodyBuilder.() -> Unit
    ): IrFunctionExpression {
        val function = compilerContext.irFactory.buildFun {
            this.startOffset = this@createLambdaExpression.startOffset
            this.endOffset = this@createLambdaExpression.endOffset
            this.returnType = type
            name = Name.identifier("<anonymous>")
            visibility = DescriptorVisibilities.LOCAL
            origin = SERIALIZABLE_PLUGIN_ORIGIN
        }
        function.body =
            DeclarationIrBuilder(compilerContext, function.symbol, startOffset, endOffset).irBlockBody(startOffset, endOffset, bodyGen)
        function.parent = this

        val f0Type = compilerContext.irBuiltIns.functionN(0)
        val f0ParamSymbol = f0Type.typeParameters[0].symbol
        val f0IrType = f0Type.defaultType.substitute(mapOf(f0ParamSymbol to type))

        return IrFunctionExpressionImpl(
            startOffset,
            endOffset,
            f0IrType,
            function,
            IrStatementOrigin.LAMBDA
        )
    }

    fun createLazyProperty(
        containingClass: IrClass,
        targetIrType: IrType,
        name: Name,
        initializerBuilder: IrBlockBodyBuilder.() -> Unit
    ): IrProperty {
        val lazySafeModeClassDescriptor = compilerContext.referenceClass(ClassId.topLevel(LAZY_MODE_FQ))!!.owner
        val lazyFunctionSymbol = compilerContext.referenceFunctions(CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("lazy"))).single {
            it.owner.valueParameters.size == 2 && it.owner.valueParameters[0].type == lazySafeModeClassDescriptor.defaultType
        }
        val publicationEntryDescriptor = lazySafeModeClassDescriptor.enumEntries().single { it.name == LAZY_PUBLICATION_MODE_NAME }

        val lazyIrClass = compilerContext.referenceClass(ClassId.topLevel(LAZY_FQ))!!.owner
        val lazyIrType = lazyIrClass.defaultType.substitute(mapOf(lazyIrClass.typeParameters[0].symbol to targetIrType))

        val propertyDescriptor =
            KSerializerDescriptorResolver.createValPropertyDescriptor(
                Name.identifier(name.asString() + "\$delegate"),
                containingClass.descriptor,
                lazyIrType.toKotlinType(),
                createGetter = true
            )

        return generateSimplePropertyWithBackingField(propertyDescriptor, containingClass).apply {
            val builder = DeclarationIrBuilder(compilerContext, containingClass.symbol, startOffset, endOffset)
            val initializerBody = builder.run {
                val enumElement = IrGetEnumValueImpl(
                    startOffset,
                    endOffset,
                    lazySafeModeClassDescriptor.defaultType,
                    publicationEntryDescriptor.symbol
                )

                val lambdaExpression = containingClass.createLambdaExpression(targetIrType, initializerBuilder)

                irExprBody(
                    irInvoke(null, lazyFunctionSymbol, listOf(targetIrType), listOf(enumElement, lambdaExpression), lazyIrType)
                )
            }
            backingField!!.initializer = initializerBody
        }
    }

    fun createCompanionValProperty(
        companionClass: IrClass,
        type: IrType,
        name: Name,
        initializerBuilder: IrBlockBodyBuilder.() -> Unit
    ): IrProperty {
        val targetKotlinType = type.toKotlinType()
        val propertyDescriptor =
            KSerializerDescriptorResolver.createValPropertyDescriptor(name, companionClass.descriptor, targetKotlinType)

        return generateSimplePropertyWithBackingField(propertyDescriptor, companionClass, name).apply {
            companionClass.contributeAnonymousInitializer {
                val irBlockBody = irBlockBody(startOffset, endOffset, initializerBuilder)
                irBlockBody.statements.dropLast(1).forEach { +it }
                val expression = irBlockBody.statements.last() as? IrExpression
                    ?: throw AssertionError("Last statement in property initializer builder is not an a expression")
                +irSetField(irGetObject(companionClass), backingField!!, expression)
            }
        }
    }

    fun IrClass.contributeAnonymousInitializer(bodyGen: IrBlockBodyBuilder.() -> Unit) {
        val symbol = IrAnonymousInitializerSymbolImpl(descriptor)
        factory.createAnonymousInitializer(startOffset, endOffset, SERIALIZABLE_PLUGIN_ORIGIN, symbol).also {
            it.parent = this
            declarations.add(it)
            it.body = DeclarationIrBuilder(compilerContext, symbol, startOffset, endOffset).irBlockBody(startOffset, endOffset, bodyGen)
        }
    }

    fun IrBlockBodyBuilder.getLazyValueExpression(thisParam: IrValueParameter, property: IrProperty, type: IrType): IrExpression {
        val lazyIrClass = compilerContext.referenceClass(ClassId.topLevel(LAZY_FQ))!!.owner
        val valueGetter = lazyIrClass.getPropertyGetter("value")!!

        val propertyGetter = property.getter!!

        return irInvoke(
            irGet(propertyGetter.returnType, irGet(thisParam), propertyGetter.symbol),
            valueGetter,
            typeHint = type
        )
    }

    fun IrBuilderWithScope.irInvoke(
        dispatchReceiver: IrExpression? = null,
        callee: IrFunctionSymbol,
        vararg args: IrExpression,
        typeHint: IrType? = null
    ): IrMemberAccessExpression<*> {
        assert(callee.isBound) { "Symbol $callee expected to be bound" }
        val returnType = typeHint ?: callee.owner.returnType
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
    ): IrMemberAccessExpression<*> =
        irInvoke(
            dispatchReceiver,
            callee,
            *valueArguments.toTypedArray(),
            typeHint = returnTypeHint
        ).also { call -> typeArguments.forEachIndexed(call::putTypeArgument) }

    fun IrBuilderWithScope.createArrayOfExpression(
        arrayElementType: IrType,
        arrayElements: List<IrExpression>
    ): IrExpression {

        val arrayType = compilerContext.irBuiltIns.arrayClass.typeWith(arrayElementType)
        val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, arrayElementType, arrayElements)
        val typeArguments = listOf(arrayElementType)

        return irCall(compilerContext.irBuiltIns.arrayOf, arrayType, typeArguments = typeArguments).apply {
            putValueArgument(0, arg0)
        }
    }

    fun IrBuilderWithScope.createPrimitiveArrayOfExpression(
        elementPrimitiveType: IrType,
        arrayElements: List<IrExpression>
    ): IrExpression {
        val arrayType = compilerContext.irBuiltIns.primitiveArrayForType.getValue(elementPrimitiveType).defaultType
        val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, elementPrimitiveType, arrayElements)
        val typeArguments = listOf(elementPrimitiveType)

        return irCall(compilerContext.irBuiltIns.arrayOf, arrayType, typeArguments = typeArguments).apply {
            putValueArgument(0, arg0)
        }
    }

    fun IrBuilderWithScope.irBinOp(name: Name, lhs: IrExpression, rhs: IrExpression): IrExpression {
        val classFqName = (lhs.type as IrSimpleType).classOrNull!!.owner.fqNameWhenAvailable!!
        val symbol = compilerContext.referenceFunctions(CallableId(ClassId.topLevel(classFqName), name)).single()
        return irInvoke(lhs, symbol, rhs)
    }

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

    @FirIncompatiblePluginAPI
    fun KotlinType.toIrType() = compilerContext.typeTranslator.translateType(this)

    fun IrBuilderWithScope.setProperty(receiver: IrExpression, property: IrProperty, value: IrExpression): IrExpression {
        return if (property.setter != null)
            irSet(property.setter!!.returnType, receiver, property.setter!!.symbol, value)
        else
            irSetField(receiver, property.backingField!!, value)
    }

    fun IrBuilderWithScope.generateAnySuperConstructorCall(toBuilder: IrBlockBodyBuilder) {
        val anyConstructor = compilerContext.irBuiltIns.anyClass.owner.declarations.single { it is IrConstructor } as IrConstructor
        with(toBuilder) {
            +IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                startOffset, endOffset,
                compilerContext.irBuiltIns.unitType,
                anyConstructor.symbol
            )
        }
    }

    private inline fun <reified T : IrDeclaration> IrClass.searchForDeclaration(descriptor: DeclarationDescriptor): T? {
        return declarations.singleOrNull { it.descriptor == descriptor } as? T
    }

    fun generateSimplePropertyWithBackingField(
        propertyDescriptor: PropertyDescriptor,
        propertyParent: IrClass,
        fieldName: Name = propertyDescriptor.name,
    ): IrProperty {
        val irProperty = propertyParent.searchForDeclaration(propertyDescriptor) ?: run {
            with(propertyDescriptor) {
                propertyParent.factory.createProperty(
                    propertyParent.startOffset, propertyParent.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrPropertySymbolImpl(propertyDescriptor),
                    name, visibility, modality, isVar, isConst, isLateInit, isDelegated, isExternal
                ).also {
                    it.parent = propertyParent
                    propertyParent.addMember(it)
                }
            }
        }

        propertyParent.generatePropertyBackingFieldIfNeeded(propertyDescriptor, irProperty, fieldName)
        val fieldSymbol = irProperty.backingField!!.symbol
        irProperty.getter = propertyDescriptor.getter?.let {
            propertyParent.generatePropertyAccessor(propertyDescriptor, irProperty, it, fieldSymbol, isGetter = true)
        }?.apply { parent = propertyParent }
        irProperty.setter = propertyDescriptor.setter?.let {
            propertyParent.generatePropertyAccessor(propertyDescriptor, irProperty, it, fieldSymbol, isGetter = false)
        }?.apply { parent = propertyParent }
        return irProperty
    }

    fun IrType.kClassToJClassIfNeeded(): IrType = this

    fun kClassExprToJClassIfNeeded(startOffset: Int, endOffset: Int, irExpression: IrExpression): IrExpression = irExpression

    private fun IrClass.generatePropertyBackingFieldIfNeeded(
        propertyDescriptor: PropertyDescriptor,
        originProperty: IrProperty,
        name: Name,
    ) {
        if (originProperty.backingField != null) return

        val field = with(propertyDescriptor) {
            @OptIn(FirIncompatiblePluginAPI::class)// should be called only with old FE
            originProperty.factory.createField(
                originProperty.startOffset, originProperty.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrFieldSymbolImpl(propertyDescriptor), name, type.toIrType(),
                visibility, !isVar, isEffectivelyExternal(), dispatchReceiverParameter == null
            )
        }
        field.apply {
            parent = this@generatePropertyBackingFieldIfNeeded
            correspondingPropertySymbol = originProperty.symbol
        }

        originProperty.backingField = field
    }

    private fun IrClass.generatePropertyAccessor(
        propertyDescriptor: PropertyDescriptor,
        property: IrProperty,
        descriptor: PropertyAccessorDescriptor,
        fieldSymbol: IrFieldSymbol,
        isGetter: Boolean,
    ): IrSimpleFunction {
        val irAccessor: IrSimpleFunction = when (isGetter) {
            true -> searchForDeclaration<IrProperty>(propertyDescriptor)?.getter
            false -> searchForDeclaration<IrProperty>(propertyDescriptor)?.setter
        } ?: run {
            with(descriptor) {
                @OptIn(FirIncompatiblePluginAPI::class) // should never be called after FIR frontend
                property.factory.createFunction(
                    fieldSymbol.owner.startOffset, fieldSymbol.owner.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrSimpleFunctionSymbolImpl(descriptor),
                    name, visibility, modality, returnType!!.toIrType(),
                    isInline, isEffectivelyExternal(), isTailrec, isSuspend, isOperator, isInfix, isExpect
                )
            }.also { f ->
                generateOverriddenFunctionSymbols(f, compilerContext.symbolTable)
                f.createParameterDeclarations(descriptor)
                @OptIn(FirIncompatiblePluginAPI::class) // should never be called after FIR frontend
                f.returnType = descriptor.returnType!!.toIrType()
                f.correspondingPropertySymbol = fieldSymbol.owner.correspondingPropertySymbol
            }
        }

        irAccessor.body = when (isGetter) {
            true -> generateDefaultGetterBody(irAccessor)
            false -> generateDefaultSetterBody(irAccessor)
        }

        return irAccessor
    }

    private fun generateDefaultGetterBody(
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val irProperty = irAccessor.correspondingPropertySymbol?.owner ?: error("Expected corresponding property for accessor ${irAccessor.render()}")

        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = irAccessor.factory.createBlockBody(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(irAccessor.dispatchReceiverParameter!!.symbol)

        val propertyIrType = irAccessor.returnType
        irBody.statements.add(
            IrReturnImpl(
                startOffset, endOffset, compilerContext.irBuiltIns.nothingType,
                irAccessor.symbol,
                IrGetFieldImpl(
                    startOffset, endOffset,
                    irProperty.backingField?.symbol ?: error("Property expected to have backing field"),
                    propertyIrType,
                    receiver
                ).let {
                    if (propertyIrType.isKClass()) {
                        irAccessor.returnType = irAccessor.returnType.kClassToJClassIfNeeded()
                        kClassExprToJClassIfNeeded(startOffset, endOffset, it)
                    } else it
                }
            )
        )
        return irBody
    }

    private fun generateDefaultSetterBody(
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val irProperty = irAccessor.correspondingPropertySymbol?.owner ?: error("Expected corresponding property for accessor ${irAccessor.render()}")
        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = irAccessor.factory.createBlockBody(startOffset, endOffset)

        val receiver = generateReceiverExpressionForFieldAccess(irAccessor.dispatchReceiverParameter!!.symbol)

        val irValueParameter = irAccessor.valueParameters.single()
        irBody.statements.add(
            IrSetFieldImpl(
                startOffset, endOffset,
                irProperty.backingField?.symbol ?: error("Property ${irProperty.render()} expected to have backing field"),
                receiver,
                IrGetValueImpl(startOffset, endOffset, irValueParameter.type, irValueParameter.symbol),
                compilerContext.irBuiltIns.unitType
            )
        )
        return irBody
    }

    fun generateReceiverExpressionForFieldAccess(
        ownerSymbol: IrValueSymbol
    ): IrExpression = IrGetValueImpl(
        ownerSymbol.owner.startOffset, ownerSymbol.owner.endOffset,
        ownerSymbol
    )

    fun IrFunction.createParameterDeclarations(
        descriptor: FunctionDescriptor,
        overwriteValueParameters: Boolean = false,
        copyTypeParameters: Boolean = true
    ) {
        val function = this
        fun irValueParameter(descriptor: ParameterDescriptor): IrValueParameter = with(descriptor) {
            @OptIn(FirIncompatiblePluginAPI::class) // should never be called after FIR frontend
            factory.createValueParameter(
                function.startOffset, function.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrValueParameterSymbolImpl(this),
                name, indexOrMinusOne, type.toIrType(), varargElementType?.toIrType(), isCrossinline, isNoinline,
                isHidden = false, isAssignable = false
            ).also {
                it.parent = function
            }
        }

        if (copyTypeParameters) {
            assert(typeParameters.isEmpty())
            copyTypeParamsFromDescriptor(descriptor)
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.let { irValueParameter(it) }
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.let { irValueParameter(it) }

        if (!overwriteValueParameters)
            assert(valueParameters.isEmpty())

        valueParameters = descriptor.valueParameters.map { irValueParameter(it) }
    }

    fun IrFunction.copyTypeParamsFromDescriptor(descriptor: FunctionDescriptor) {
        val newTypeParameters = descriptor.typeParameters.map {
            factory.createTypeParameter(
                startOffset, endOffset,
                SERIALIZABLE_PLUGIN_ORIGIN,
                IrTypeParameterSymbolImpl(it),
                it.name, it.index, it.isReified, it.variance
            ).also { typeParameter ->
                typeParameter.parent = this
            }
        }
        @OptIn(FirIncompatiblePluginAPI::class) // should never be called after FIR frontend
        newTypeParameters.forEach { typeParameter ->
            typeParameter.superTypes = typeParameter.descriptor.upperBounds.map { it.toIrType() }
        }

        typeParameters = newTypeParameters
    }

    fun createClassReference(classType: IrType, startOffset: Int, endOffset: Int): IrClassReference {
        return IrClassReferenceImpl(
            startOffset,
            endOffset,
            compilerContext.irBuiltIns.kClassClass.starProjectedType,
            classType.classifierOrFail,
            classType
        )
    }

    fun IrBuilderWithScope.classReference(classSymbol: IrClassSymbol): IrClassReference =
        createClassReference(classSymbol.starProjectedType, startOffset, endOffset)

    fun collectSerialInfoAnnotations(irClass: IrClass): List<IrConstructorCall> {
        if (!(irClass.isInterface || irClass.descriptor.hasSerializableOrMetaAnnotation)) return emptyList()
        val annotationByFq: MutableMap<FqName, IrConstructorCall> =
            irClass.annotations.associateBy { it.symbol.owner.parentAsClass.descriptor.fqNameSafe }.toMutableMap()
        for (clazz in irClass.getAllSuperclasses()) {
            val annotations = clazz.annotations
                .mapNotNull {
                    val descriptor = it.symbol.owner.parentAsClass.descriptor
                    if (descriptor.isInheritableSerialInfoAnnotation) descriptor.fqNameSafe to it else null
                }
            annotations.forEach { (fqname, call) ->
                if (fqname !in annotationByFq) {
                    annotationByFq[fqname] = call
                } else {
                    // SerializationPluginDeclarationChecker already reported inconsistency
                }
            }
        }
        return annotationByFq.values.toList()
    }

    fun IrBuilderWithScope.copyAnnotationsFrom(annotations: List<IrConstructorCall>): List<IrExpression> =
        annotations.mapNotNull { annotationCall ->
            val annotationClass = annotationCall.symbol.owner.parentAsClass
            if (!annotationClass.descriptor.isSerialInfoAnnotation) return@mapNotNull null

            if (compilerContext.platform.isJvm()) {
                val implClass = compilerContext.serialInfoImplJvmIrGenerator.getImplClass(annotationClass)
                val ctor = implClass.constructors.singleOrNull { it.valueParameters.size == annotationCall.valueArgumentsCount }
                    ?: error("No constructor args found for SerialInfo annotation Impl class: ${implClass.render()}")
                irCall(ctor).apply {
                    for (i in 0 until annotationCall.valueArgumentsCount) {
                        val argument = annotationCall.getValueArgument(i)
                            ?: annotationClass.primaryConstructor!!.valueParameters[i].defaultValue?.expression
                        putValueArgument(i, argument!!.deepCopyWithVariables())
                    }
                }
            } else {
                annotationCall.deepCopyWithVariables()
            }
        }

    fun IrBuilderWithScope.wrapperClassReference(classType: IrType): IrClassReference {
        if (compilerContext.platform.isJvm()) {
            // "Byte::class" -> "java.lang.Byte::class"
//          TODO: get rid of descriptor
            val wrapperFqName =
                KotlinBuiltIns.getPrimitiveType(classType.classOrNull!!.descriptor)?.let(JvmPrimitiveType::get)?.wrapperFqName
            if (wrapperFqName != null) {
                val wrapperClass = compilerContext.referenceClass(ClassId.topLevel(wrapperFqName))
                    ?: error("Primitive wrapper class for $classType not found: $wrapperFqName")
                return createClassReference(wrapperClass.defaultType, startOffset, endOffset)
            }
        }
        return createClassReference(classType, startOffset, endOffset)
    }

    fun IrClass.getSuperClassOrAny(): IrClass = getSuperClassNotAny() ?: compilerContext.irBuiltIns.anyClass.owner
}