/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.backend.common.*
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.*
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

interface IrBuilderExtension {
    val compilerContext: SerializationPluginContext

    private val throwMissedFieldExceptionFunc
        get() = compilerContext.referenceFunctions(SerialEntityNames.SINGLE_MASK_FIELD_MISSING_FUNC_FQ).singleOrNull()

    private val throwMissedFieldExceptionArrayFunc
        get() = compilerContext.referenceFunctions(SerialEntityNames.ARRAY_MASK_FIELD_MISSING_FUNC_FQ).singleOrNull()

    private inline fun <reified T : IrDeclaration> IrClass.searchForDeclaration(descriptor: DeclarationDescriptor): T? {
        return declarations.singleOrNull { it.descriptor == descriptor } as? T
    }

    fun useFieldMissingOptimization(): Boolean {
        return throwMissedFieldExceptionFunc != null && throwMissedFieldExceptionArrayFunc != null
    }

    fun IrClass.contributeFunction(descriptor: FunctionDescriptor, bodyGen: IrBlockBodyBuilder.(IrFunction) -> Unit) {
        val f: IrSimpleFunction = searchForDeclaration(descriptor) ?: compilerContext.symbolTable.referenceSimpleFunction(descriptor).owner
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
        val c: IrConstructor = searchForDeclaration(descriptor) ?: compilerContext.symbolTable.referenceConstructor(descriptor).owner
        c.body = DeclarationIrBuilder(compilerContext, c.symbol, this.startOffset, this.endOffset).irBlockBody(
            this.startOffset,
            this.endOffset
        ) { bodyGen(c) }
    }

    // function will not be created in the real class
    fun IrClass.createInlinedFunction(
        name: Name,
        visibility: DescriptorVisibility,
        origin: IrDeclarationOrigin,
        returnType: IrType,
        bodyGen: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrSimpleFunction {
        val function = factory.buildFun {
            this.name = name
            this.visibility = visibility
            this.origin = origin
            this.isInline = true
            this.returnType = returnType
        }
        val functionSymbol = function.symbol
        function.parent = this
        function.body = DeclarationIrBuilder(compilerContext, functionSymbol, startOffset, endOffset).irBlockBody(
            startOffset,
            endOffset
        ) { bodyGen(function) }
        return function
    }

    fun IrClass.createSingletonLambda(
        returnType: IrType,
        startOffset: Int,
        endOffset: Int,
        bodyGen: IrBlockBodyBuilder.() -> Unit
    ): IrSimpleFunction {
        val function = compilerContext.irFactory.buildFun {
            this.startOffset = startOffset
            this.endOffset = endOffset
            this.returnType = returnType
            name = Name.identifier("<anonymous>")
            visibility = DescriptorVisibilities.LOCAL
            origin = SERIALIZABLE_PLUGIN_ORIGIN
        }
        function.body =
            DeclarationIrBuilder(compilerContext, function.symbol, startOffset, endOffset).irBlockBody(startOffset, endOffset, bodyGen)

        return function
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

        return irCall(compilerContext.symbols.arrayOf, arrayType, typeArguments = typeArguments).apply {
            putValueArgument(0, arg0)
        }
    }

    fun ClassDescriptor.referenceFunctionSymbol(
        functionName: String,
        predicate: (IrSimpleFunction) -> Boolean = { true }
    ): IrFunctionSymbol {
        val irClass = compilerContext.referenceClass(fqNameSafe)?.owner ?: error("Couldn't load class $this")
        val simpleFunctions = irClass.declarations.filterIsInstance<IrSimpleFunction>()

        return simpleFunctions.filter { it.name.asString() == functionName }.single { predicate(it) }.symbol
    }

    fun IrBuilderWithScope.createPrimitiveArrayOfExpression(
        elementPrimitiveType: IrType,
        arrayElements: List<IrExpression>
    ): IrExpression {
        val arrayType = compilerContext.irBuiltIns.primitiveArrayForType.getValue(elementPrimitiveType).defaultType
        val arg0 = IrVarargImpl(startOffset, endOffset, arrayType, elementPrimitiveType, arrayElements)
        val typeArguments = listOf(elementPrimitiveType)

        return irCall(compilerContext.symbols.arrayOf, arrayType, typeArguments = typeArguments).apply {
            putValueArgument(0, arg0)
        }
    }

    fun IrBuilderWithScope.irBinOp(name: Name, lhs: IrExpression, rhs: IrExpression): IrExpression {
        val classFqName = (lhs.type as IrSimpleType).classOrNull!!.owner.fqNameWhenAvailable!!
        val symbol = compilerContext.referenceFunctions(classFqName.child(name)).single()
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

    fun IrClass.searchForProperty(descriptor: PropertyDescriptor): IrProperty {
        // this API is used to reference both current module descriptors and external ones (because serializable class can be in any of them),
        // so we use descriptor api for current module because it is not possible to obtain FQname for e.g. local classes.
        return searchForDeclaration(descriptor) ?: if (descriptor.module == compilerContext.moduleDescriptor) {
            compilerContext.symbolTable.referenceProperty(descriptor).owner
        } else {
            compilerContext.referenceProperties(descriptor.fqNameSafe).single().owner
        }
    }

    fun SerializableProperty.getIrPropertyFrom(thisClass: IrClass): IrProperty {
        return thisClass.searchForProperty(descriptor)
    }


    /*
      Create a function that creates `get property value expressions` for given corresponded constructor's param
        (constructor_params) -> get_property_value_expression
     */
    fun IrBuilderWithScope.createPropertyByParamReplacer(
        irClass: IrClass,
        serialProperties: List<SerializableProperty>,
        instance: IrValueParameter,
        bindingContext: BindingContext
    ): (ValueParameterDescriptor) -> IrExpression? {
        fun SerializableProperty.irGet(): IrExpression {
            val ownerType = instance.symbol.owner.type
            return getProperty(
                irGet(
                    type = ownerType,
                    variable = instance.symbol
                ), getIrPropertyFrom(irClass)
            )
        }

        val serialPropertiesMap = serialProperties.associateBy { it.descriptor }

        val transientPropertiesMap =
            irClass.declarations.asSequence()
                .filterIsInstance<IrProperty>()
                .filter { it.backingField != null }.filter { !serialPropertiesMap.containsKey(it.descriptor) }
                .associateBy { it.symbol.descriptor }

        return {
            val propertyDescriptor = bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, it]
            if (propertyDescriptor != null) {
                val value = serialPropertiesMap[propertyDescriptor]
                value?.irGet() ?: transientPropertiesMap[propertyDescriptor]?.let { prop ->
                    getProperty(
                        irGet(instance),
                        prop
                    )
                }
            } else {
                null
            }
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
            +IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
                startOffset, endOffset,
                compilerContext.irBuiltIns.unitType,
                anyConstructor.symbol
            )
        }
    }

    fun IrBlockBodyBuilder.generateGoldenMaskCheck(
        seenVars: List<IrValueDeclaration>,
        properties: SerializableProperties,
        serialDescriptor: IrExpression
    ) {
        val fieldsMissedTest: IrExpression
        val throwErrorExpr: IrExpression

        val maskSlotCount = seenVars.size
        if (maskSlotCount == 1) {
            val goldenMask = properties.goldenMask


            throwErrorExpr = irInvoke(
                null,
                throwMissedFieldExceptionFunc!!,
                irGet(seenVars[0]),
                irInt(goldenMask),
                serialDescriptor,
                typeHint = compilerContext.irBuiltIns.unitType
            )

            fieldsMissedTest = irNotEquals(
                irInt(goldenMask),
                irBinOp(
                    OperatorNameConventions.AND,
                    irInt(goldenMask),
                    irGet(seenVars[0])
                )
            )
        } else {
            val goldenMaskList = properties.goldenMaskList

            var compositeExpression: IrExpression? = null
            for (i in goldenMaskList.indices) {
                val singleCheckExpr = irNotEquals(
                    irInt(goldenMaskList[i]),
                    irBinOp(
                        OperatorNameConventions.AND,
                        irInt(goldenMaskList[i]),
                        irGet(seenVars[i])
                    )
                )

                compositeExpression = if (compositeExpression == null) {
                    singleCheckExpr
                } else {
                    irBinOp(
                        OperatorNameConventions.OR,
                        compositeExpression,
                        singleCheckExpr
                    )
                }
            }

            fieldsMissedTest = compositeExpression!!

            throwErrorExpr = irBlock {
                +irInvoke(
                    null,
                    throwMissedFieldExceptionArrayFunc!!,
                    createPrimitiveArrayOfExpression(compilerContext.irBuiltIns.intType, goldenMaskList.indices.map { irGet(seenVars[it]) }),
                    createPrimitiveArrayOfExpression(compilerContext.irBuiltIns.intType, goldenMaskList.map { irInt(it) }),
                    serialDescriptor,
                    typeHint = compilerContext.irBuiltIns.unitType
                )
            }
        }

        +irIfThen(compilerContext.irBuiltIns.unitType, fieldsMissedTest, throwErrorExpr)
    }

    fun generateSimplePropertyWithBackingField(
        propertyDescriptor: PropertyDescriptor,
        propertyParent: IrClass,
        fieldName: Name = propertyDescriptor.name,
    ): IrProperty {
        val irProperty = propertyParent.searchForDeclaration<IrProperty>(propertyDescriptor) ?: run {
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

    private fun IrClass.generatePropertyBackingFieldIfNeeded(
        propertyDescriptor: PropertyDescriptor,
        originProperty: IrProperty,
        name: Name,
    ) {
        if (originProperty.backingField != null) return

        val field = with(propertyDescriptor) {
            // TODO: type parameters
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
                property.factory.createFunction(
                    fieldSymbol.owner.startOffset, fieldSymbol.owner.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, IrSimpleFunctionSymbolImpl(descriptor),
                    name, visibility, modality, returnType!!.toIrType(),
                    isInline, isEffectivelyExternal(), isTailrec, isSuspend, isOperator, isInfix, isExpect
                )
            }.also { f ->
                generateOverriddenFunctionSymbols(f, compilerContext.symbolTable)
                f.createParameterDeclarations(receiver = null)
                f.returnType = descriptor.returnType!!.toIrType()
                f.correspondingPropertySymbol = fieldSymbol.owner.correspondingPropertySymbol
            }
        }

        irAccessor.body = when (isGetter) {
            true -> generateDefaultGetterBody(descriptor as PropertyGetterDescriptor, irAccessor)
            false -> generateDefaultSetterBody(descriptor as PropertySetterDescriptor, irAccessor)
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
        val irBody = irAccessor.factory.createBlockBody(startOffset, endOffset)

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
        val irBody = irAccessor.factory.createBlockBody(startOffset, endOffset)

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
        val function = this
        fun irValueParameter(descriptor: ParameterDescriptor): IrValueParameter = with(descriptor) {
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
            copyTypeParamsFromDescriptor()
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.let { irValueParameter(it) }
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.let { irValueParameter(it) }

        if (!overwriteValueParameters)
            assert(valueParameters.isEmpty())

        valueParameters = descriptor.valueParameters.map { irValueParameter(it) }
    }

    fun IrFunction.copyTypeParamsFromDescriptor() {
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

        newTypeParameters.forEach { typeParameter ->
            typeParameter.superTypes = typeParameter.descriptor.upperBounds.map { it.toIrType() }
        }

        typeParameters = newTypeParameters
    }

    fun kClassTypeFor(projection: TypeProjection): SimpleType {
        val kClass = compilerContext.builtIns.kClass
        return KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, kClass, listOf(projection))
    }

    fun createClassReference(classType: KotlinType, startOffset: Int, endOffset: Int): IrClassReference {
        val clazz = classType.toClassDescriptor!!
        val classSymbol = compilerContext.referenceClass(clazz.fqNameSafe) ?: error("Couldn't load class $clazz")
        return IrClassReferenceImpl(
            startOffset,
            endOffset,
            compilerContext.irBuiltIns.kClassClass.starProjectedType,
            classSymbol,
            classSymbol.starProjectedType
        )
    }

    fun IrBuilderWithScope.classReference(classType: KotlinType): IrClassReference = createClassReference(classType, startOffset, endOffset)

    private fun extractDefaultValuesFromConstructor(irClass: IrClass?): Map<ParameterDescriptor, IrExpression?> {
        if (irClass == null) return emptyMap()
        val original = irClass.constructors.singleOrNull { it.isPrimary }
        // default arguments of original constructor
        val defaultsMap: Map<ParameterDescriptor, IrExpression?> =
            original?.valueParameters?.associate { it.descriptor to it.defaultValue?.expression } ?: emptyMap()
        return defaultsMap + extractDefaultValuesFromConstructor(irClass.getSuperClassNotAny())
    }

    /*
    Creates an initializer adapter function that can replace IR expressions of getting constructor parameter value by some other expression.
    Also adapter may replace IR expression of getting `this` value by another expression.
     */
    fun createInitializerAdapter(
        irClass: IrClass,
        paramGetReplacer: (ValueParameterDescriptor) -> IrExpression?,
        thisGetReplacer: Pair<IrValueSymbol, () -> IrExpression>? = null
    ): (IrExpressionBody) -> IrExpression {
        val initializerTransformer = object : IrElementTransformerVoid() {
            // try to replace `get some value` expression
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val symbol = expression.symbol
                if (thisGetReplacer != null && thisGetReplacer.first == symbol) {
                    // replace `get this value` expression
                    return thisGetReplacer.second()
                }

                val descriptor = symbol.descriptor
                if (descriptor is ValueParameterDescriptor) {
                    // replace `get parameter value` expression
                    paramGetReplacer(descriptor)?.let { return it }
                }

                // otherwise leave expression as it is
                return super.visitGetValue(expression)
            }
        }
        val defaultsMap = extractDefaultValuesFromConstructor(irClass)
        return fun(initializer: IrExpressionBody): IrExpression {
            val rawExpression = initializer.expression
            val expression =
                if (rawExpression is IrGetValueImpl && rawExpression.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER) {
                    // this is a primary constructor property, use corresponding default of value parameter
                    defaultsMap.getValue(rawExpression.symbol.descriptor as ParameterDescriptor)!!
                } else {
                    rawExpression
                }
            return expression.deepCopyWithVariables().transform(initializerTransformer, null)
        }
    }

    fun findEnumValuesMethod(enumClass: ClassDescriptor): IrFunction {
        assert(enumClass.kind == ClassKind.ENUM_CLASS)
        return compilerContext.referenceClass(enumClass.fqNameSafe)?.let {
            it.owner.functions.singleOrNull { f ->
                f.name == Name.identifier("values") && f.valueParameters.isEmpty() && f.extensionReceiverParameter == null
            } ?: throw AssertionError("Enum class does not have single .values() function")
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
            ?.let { expr -> wrapWithNullableSerializerIfNeeded(property.type, expr, nullableSerClass) }
    }

    private fun IrBuilderWithScope.wrapWithNullableSerializerIfNeeded(
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
        val (_, ir) = enclosingGenerator.localSerializersFieldsDescriptors[it]
        irGetField(irGet(dispatchReceiverParameter), ir.backingField!!)
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
        }
        fun instantiate(serializer: ClassDescriptor?, type: KotlinType): IrExpression? {
            val expr = serializerInstance(
                enclosingGenerator,
                serializer,
                module,
                type,
                type.genericIndex,
                genericGetter
            ) ?: return null
            return wrapWithNullableSerializerIfNeeded(type, expr, nullableSerClass)
        }

        var serializerClass = serializerClassOriginal
        var args: List<IrExpression>
        var typeArgs: List<IrType?>
        val thisIrType = kType.toIrType()
        val hasNewCtxSerCtor =
            serializerClassOriginal.classId == contextSerializerId && compilerContext.referenceConstructors(serializerClass.fqNameSafe)
                .any { it.owner.valueParameters.size == 3 }
        when (serializerClassOriginal.classId) {
            contextSerializerId, polymorphicSerializerId -> {
                args = listOf(classReference(kType))
                typeArgs = listOf(thisIrType)

                if (hasNewCtxSerCtor) {
                    // new signature of context serializer
                    args = args + mutableListOf<IrExpression>().apply {
                        val fallbackDefaultSerializer = findTypeSerializer(module, kType)
                        add(instantiate(fallbackDefaultSerializer, kType) ?: irNull())
                        add(
                            createArrayOfExpression(
                                wrapIrTypeIntoKSerializerIrType(
                                    module,
                                    thisIrType,
                                    variance = Variance.OUT_VARIANCE
                                ),
                                kType.arguments.map {
                                    val argSer = enclosingGenerator.findTypeSerializerOrContext(
                                        module,
                                        it.type,
                                        sourceElement = serializerClassOriginal.findPsi()
                                    )
                                    instantiate(argSer, it.type)!!
                                })
                        )
                    }
                }
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
                                wrapWithNullableSerializerIfNeeded(type, expr, nullableSerClass)
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
            args = listOf(wrapperClassReference(kType.arguments.single().type)) + args
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

    private fun IrBuilderWithScope.wrapperClassReference(classType: KotlinType): IrClassReference {
        if (compilerContext.platform.isJvm()) {
            // "Byte::class" -> "java.lang.Byte::class"
            val wrapperFqName = KotlinBuiltIns.getPrimitiveType(classType)?.let(JvmPrimitiveType::get)?.wrapperFqName
            if (wrapperFqName != null) {
                val wrapperClass = compilerContext.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(wrapperFqName))
                    ?: error("Primitive wrapper class for $classType not found: $wrapperFqName")
                return classReference(wrapperClass.defaultType)
            }
        }
        return classReference(classType)
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
            compilerContext.referenceClass(SerializationPackages.internalPackageFqName.child(SerialEntityNames.SERIAL_CTOR_MARKER_NAME))

        return valueParameters.lastOrNull()?.run {
            name == SerialEntityNames.dummyParamName && type.classifierOrNull == serialMarker
        } == true
    }

    fun serializableSyntheticConstructor(forClass: IrClass): IrConstructorSymbol {
        return forClass.declarations.filterIsInstance<IrConstructor>().single { it.isSerializationCtor() }.symbol
    }

    fun IrClass.getSuperClassOrAny(): IrClass = getSuperClassNotAny() ?: compilerContext.irBuiltIns.anyClass.owner

    fun IrClass.getSuperClassNotAny(): IrClass? {
        val superClasses = superTypes.mapNotNull { it.classOrNull }.map { it.owner }

        return superClasses.singleOrNull { it.kind == ClassKind.CLASS }
    }

}
