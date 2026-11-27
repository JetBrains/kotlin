/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.jvm.ir.isKotlinValhallaValueClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.v2.irVariable
import org.jetbrains.kotlin.ir.builders.expressions.v2.IrDelegatingConstructorCallBuilder
import org.jetbrains.kotlin.ir.builders.expressions.v2.irBranch
import org.jetbrains.kotlin.ir.builders.expressions.v2.irCall
import org.jetbrains.kotlin.ir.builders.expressions.v2.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.expressions.v2.irElseBranch
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetField
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetObjectValue
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetValue
import org.jetbrains.kotlin.ir.builders.expressions.v2.irSetField
import org.jetbrains.kotlin.ir.builders.expressions.v2.irWhen
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.getOrPutNullable
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.CACHED_DESCRIPTOR_FIELD_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.LOAD
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SAVE
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESC_FIELD

class SerializableIrGenerator(
    val irClass: IrClass,
    compilerContext: SerializationPluginContext
) : BaseIrGenerator(irClass, compilerContext) {

    protected val properties = serializablePropertiesForIrBackend(irClass)

    private val serialDescriptorClass = compilerContext.finderForBuiltins().findClass(
        SerializationRuntimeClassIds.descriptorClassId
    )!!.owner


    private val serialDescriptorImplClass = compilerContext.finderForBuiltins().findClass(
        ClassId(
            SerializationPackages.internalPackageFqName,
            Name.identifier(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL)
        )
    )!!.owner

    private val addElementFun =
        serialDescriptorImplClass.findDeclaration<IrFunction> { it.name.toString() == CallingConventions.addElement }!!.symbol

    private val IrClass.isInternalSerializable: Boolean get() = kind == ClassKind.CLASS && hasSerializableOrMetaAnnotationWithoutArgs()

    private val cachedChildSerializers: List<IrExpression?> =
        irClass.companionObject()!!.createCachedChildSerializers(irClass, properties.serializableProperties)

    private val cachedChildSerializersProperty: IrProperty? =
        irClass.companionObject()!!.addCachedChildSerializersProperty(cachedChildSerializers)


    fun generateInternalConstructor(constructorDescriptor: IrConstructor) =
        addFunctionBody(constructorDescriptor) { ctor ->
            val thiz = irClass.thisReceiver!!
            val serializableProperties = properties.serializableProperties

            // Like primary constructors in fir2ir, final full value classes assign their fields before `super(...)` if they have
            // a non-Any (value class) superclass, and under Project Valhalla (JVM) always, since their strict fields (JEP 401) must be
            // definitely assigned before `super()` even when they extend `Any`. Other value classes keep the plain order, as e.g. JS/ES6
            // doesn't allow using `this` before `super()`.
            val assignFieldsBeforeSuper = irClass.isFullValueClass && irClass.isFinalClass &&
                    (irClass.getSuperClassNotAny() != null || irClass.isKotlinValhallaValueClass(compilerContext.languageVersionSettings))
            // `this` can't be read before super(), so defaults read the preceding properties from local variables.
            val valuesBeforeSuper = mutableMapOf<Name, IrVariable>()
            val propertyByParamReplacer: (IrValueParameterSymbol) -> IrExpression? =
                if (assignFieldsBeforeSuper) {
                    { parameter -> valuesBeforeSuper[parameter.owner.name]?.let(::irGet) }
                } else {
                    createPropertyByParamReplacer(irClass, serializableProperties, thiz)
                }

            val initializerAdapter: (IrExpressionBody) -> IrExpression = createInitializerAdapter(irClass, propertyByParamReplacer)

            val statementsAfterSerializableProperty = collectStatementsAfterSerializableProperties(thiz, initializerAdapter)

            val seenVarsOffset = serializableProperties.bitMaskSlotCount()
            val seenVars = (0 until seenVarsOffset).map { ctor.parameters[it] }

            val superClass = irClass.getSuperClassOrAny()
            val startPropOffset =
                if (superClass.shouldHaveGeneratedMethods()) serializablePropertiesForIrBackend(superClass).serializableProperties.size else 0

            if (!irClass.isAbstractOrSealedSerializableClass) {
                generateGoldenMaskCheck(seenVars, properties, getSerialDescriptorExprForConstructor())
            }

            if (assignFieldsBeforeSuper) {
                generateFieldAssignmentsBeforeSuper(thiz, ctor.parameters, seenVars, initializerAdapter, valuesBeforeSuper)
            }

            val indicesAssignedAfterSuper = if (assignFieldsBeforeSuper) IntRange.EMPTY else startPropOffset until serializableProperties.size
            val propertyAssignments = indicesAssignedAfterSuper.associate { index ->
                serializableProperties[index].ir to generatePropertyAssignment(thiz, index, ctor.parameters, seenVars, initializerAdapter)
            }

            generateSuperConstructorCall(superClass, ctor.parameters, seenVarsOffset)

            statementsAfterSerializableProperty[null]?.forEach { +it }
            for (index in startPropOffset until serializableProperties.size) {
                val property = serializableProperties[index].ir
                propertyAssignments[property]?.let { +it }
                statementsAfterSerializableProperty[property]?.forEach { +it }
            }

            generateDelegateFieldInitializers(thiz, initializerAdapter)
        }

    private fun IrBlockBodyBuilder.collectStatementsAfterSerializableProperties(
        thiz: IrValueParameter,
        initializerAdapter: (IrExpressionBody) -> IrExpression,
    ): Map<IrProperty?, List<IrStatement>> {
        val serialDescs = properties.serializableProperties.map { it.ir }.toSet()
        var current: IrProperty? = null
        val statementsAfterSerializableProperty: MutableMap<IrProperty?, MutableList<IrStatement>> = mutableMapOf()
        irClass.declarations.asSequence().forEach {
            when {
                // only properties with backing field
                it is IrProperty && it.isNonStaticWithField -> {
                    if (it in serialDescs) {
                        current = it
                    } else if (it.backingField?.initializer != null &&
                        !(it.isJvmOptimizableDelegate() && compilerContext.platform.isJvm())
                    ) {
                        // skip transient lateinit or deferred properties (with null initializer) and optimized delegations
                        val expression = initializerAdapter(it.backingField!!.initializer!!)

                        statementsAfterSerializableProperty.getOrPutNullable(current, { mutableListOf() })
                            .add(irSetField(it.backingField!!.symbol) {
                                receiver = irGetValue(thiz.symbol)
                                value = expression
                            })
                    }
                }
                it is IrAnonymousInitializer -> {
                    val statements = it.body.deepCopyWithoutPatchingParents().statements
                    statementsAfterSerializableProperty.getOrPutNullable(current, { mutableListOf() })
                        .addAll(statements)
                }
            }
        }
        return statementsAfterSerializableProperty
    }

    private fun IrBlockBodyBuilder.getSerialDescriptorExprForConstructor(): IrExpression =
        if (irClass.isStaticSerializable) {
            getStaticSerialDescriptorExprForConstructor()
        } else {
            // synthetic constructor is created only for internally serializable classes - so companion definitely exists
            val companionObject = irClass.companionObject()!!
            getParametrizedSerialDescriptorExpr(companionObject, createCachedDescriptorProperty(companionObject))
        }

    private fun IrBlockBodyBuilder.propertyNotSeenTest(seenVars: List<IrValueParameter>, index: Int): IrExpression =
        irCall(context.irBuiltIns.eqeqSymbol) {
            origin = IrStatementOrigin.EQEQ
            arguments[0] = irInt(0)
            arguments[1] = irBinOp(
                OperatorNameConventions.AND,
                irGetValue(seenVars[bitMaskSlotAt(index)].symbol),
                irInt(1 shl (index % 32))
            )
        }

    private fun defaultValueOf(prop: IrSerializableProperty, initializerAdapter: (IrExpressionBody) -> IrExpression): IrExpression =
        requireNotNull(initializerAdapter(prop.ir.defaultValueInitializer()!!)) { "Optional value without an initializer" } // todo: filter abstract here

    private fun IrBlockBodyBuilder.generateFieldAssignmentsBeforeSuper(
        thiz: IrValueParameter,
        parameters: List<IrValueParameter>,
        seenVars: List<IrValueParameter>,
        initializerAdapter: (IrExpressionBody) -> IrExpression,
        valuesBeforeSuper: MutableMap<Name, IrVariable>,
    ) {
        val serializableProperties = properties.serializableProperties
        // No branch may follow an assignment of a strict field before super(): ASM doesn't track unset strict fields in frames.
        val fieldAssignments = mutableListOf<IrExpression>()
        for (property in irClass.properties.filter { it.isNonStaticWithField }) {
            val index = serializableProperties.indexOfFirst { it.ir == property }
            val prop = serializableProperties.getOrNull(index)
            val value = when {
                // Transient properties get their defaults.
                prop == null -> initializerAdapter(property.defaultValueInitializer() ?: continue)
                prop.optional -> irWhen {
                    type = property.backingField!!.type
                    branches += irBranch {
                        condition = propertyNotSeenTest(seenVars, index)
                        result = defaultValueOf(prop, initializerAdapter)
                    }
                    branches += irElseBranch {
                        result = irGetValue(parameters[index + seenVars.size].symbol)
                    }
                }
                else -> irGetValue(parameters[index + seenVars.size].symbol)
            }
            val variable = irVariable {
                initializer = value
                name = Name.identifier(scope.inventNameForTemporary(nameHint = property.name.asString()))
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            }.also { +it }
            valuesBeforeSuper[property.name] = variable
            fieldAssignments += irSetField(property.backingField!!.symbol) {
                receiver = irGetValue(thiz.symbol)
                this.value = irGetValue(variable.symbol)
            }
        }
        fieldAssignments.forEach { +it }
    }

    private fun IrBlockBodyBuilder.generatePropertyAssignment(
        thiz: IrValueParameter,
        index: Int,
        parameters: List<IrValueParameter>,
        seenVars: List<IrValueParameter>,
        initializerAdapter: (IrExpressionBody) -> IrExpression,
    ): IrExpression {
        val prop = properties.serializableProperties[index]
        val paramRef = parameters[index + seenVars.size]
        // Assign this.a = a in else branch
        // Set field directly w/o setter to match behavior of old backend plugin
        val backingFieldToAssign = prop.ir.backingField!!
        val assignParamExpr = irSetField(backingFieldToAssign.symbol) {
            receiver = irGetValue(thiz.symbol)
            value = irGetValue(paramRef.symbol)
        }

        if (!prop.optional) {
            // property required
            // field definitely not empty as it's checked before - no need another IF, only assign property from param
            return assignParamExpr
        }
        val ifNotSeenExpr = irSetField(backingFieldToAssign.symbol) {
            receiver = irGetValue(thiz.symbol)
            value = defaultValueOf(prop, initializerAdapter)
        }
        return irWhen {
            type = compilerContext.irBuiltIns.unitType
            branches += irBranch {
                condition = propertyNotSeenTest(seenVars, index)
                result = ifNotSeenExpr
            }
            branches += irElseBranch {
                result = assignParamExpr
            }
        }
    }

    private fun IrBlockBodyBuilder.generateSuperConstructorCall(
        superClass: IrClass,
        allValueParameters: List<IrValueParameter>,
        seenVarsOffset: Int,
    ) {
        when {
            superClass.symbol == compilerContext.irBuiltIns.anyClass -> generateAnySuperConstructorCall(toBuilder = this)
            superClass.shouldHaveGeneratedMethods() -> generateSuperSerializableCall(superClass, allValueParameters, seenVarsOffset)
            else -> generateSuperNonSerializableCall(superClass)
        }
    }

    // Handle function-intialized interface delegates
    private fun IrBlockBodyBuilder.generateDelegateFieldInitializers(
        thiz: IrValueParameter,
        initializerAdapter: (IrExpressionBody) -> IrExpression,
    ) {
        irClass.declarations
            .filterIsInstance<IrField>()
            .filter { it.origin == IrDeclarationOrigin.DELEGATE }
            .forEach {
                val receiver = if (!it.isStatic) irGetValue(thiz.symbol) else null
                +irSetField(
                    receiver,
                    it,
                    initializerAdapter(it.initializer!!),
                    IrStatementOrigin.INITIALIZE_FIELD
                )
            }
    }

    private fun IrBlockBodyBuilder.getStaticSerialDescriptorExprForConstructor(): IrExpression {
        val serializerIrClass = if (!irClass.hasKeepGeneratedSerializerAnnotation) {
            irClass.classSerializer(compilerContext)!!.owner
            // internally generated serializer always declared inside serializable class
        } else {
            // if KeepGeneratedSerializer is specified, constructors are always created for kept serializer
            irClass.generatedSerializer?.owner
                ?: throw IllegalStateException("Generated serializer for $irClass with keep serializer was not found ")
        }

        val serialDescriptorGetter =
            serializerIrClass.getPropertyGetter(SERIAL_DESC_FIELD)!!.owner
        return irCall(serialDescriptorGetter.symbol) {
            type = serialDescriptorGetter.returnType
            origin = IrStatementOrigin.GET_PROPERTY
            arguments[0] = irGetObjectValue(serializerIrClass.symbol)
        }
    }

    private fun IrBlockBodyBuilder.getParametrizedSerialDescriptorExpr(companionObject: IrClass, property: IrProperty): IrExpression {
        return irGetField(property.backingField!!.symbol) { receiver = irGetObjectValue(companionObject.symbol) }
    }

    private fun createCachedDescriptorProperty(companionObject: IrClass): IrProperty {
        val serialDescIrType = serialDescriptorClass.defaultType

        return companionObject.addValPropertyWithJvmField(serialDescIrType, CACHED_DESCRIPTOR_FIELD_NAME, DescriptorVisibilities.PUBLIC) {
            val serialDescVar = irVariable {
                initializer = getInstantiateDescriptorExpr()
                name = Name.identifier(scope.inventNameForTemporary(nameHint = "serialDesc"))
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            }.also { +it }
            for (property in properties.serializableProperties) {
                +getAddElementToDescriptorExpr(property, serialDescVar)
            }
            +irGetValue(serialDescVar.symbol)
        }
    }

    private fun IrBlockBodyBuilder.getInstantiateDescriptorExpr(): IrExpression {
        val classConstructors = serialDescriptorImplClass.constructors
        val serialClassDescImplCtor = classConstructors.single { it.isPrimary }.symbol
        return irInvoke(
            serialClassDescImplCtor,
            irString(irClass.serialName()), irNull(), irInt(properties.serializableProperties.size)
        )
    }

    private fun IrBlockBodyBuilder.getAddElementToDescriptorExpr(
        property: IrSerializableProperty,
        serialDescVar: IrVariable
    ): IrExpression {
        return irInvoke(
            addElementFun,
            irGetValue(serialDescVar.symbol),
            irString(property.name),
            irBoolean(property.optional),
            returnTypeHint = compilerContext.irBuiltIns.unitType
        )
    }

    private fun IrBlockBodyBuilder.generateSuperNonSerializableCall(superClass: IrClass) {
        val ctorRef = superClass.declarations.filterIsInstance<IrConstructor>().singleOrNull { it.parameters.isEmpty() }
            ?: error("Non-serializable parent of serializable $irClass must have no arg constructor")


        +irDelegatingConstructorCall(ctorRef.symbol) {
            insertTypeArgumentsForSuperClass(superClass)
        }
    }

    private fun IrDelegatingConstructorCallBuilder.insertTypeArgumentsForSuperClass(superClass: IrClass) {
        val superTypeCallArguments = (irClass.superTypes.find { it.classOrNull == superClass.symbol } as IrSimpleType?)?.arguments
        superTypeCallArguments?.forEachIndexed { index, irTypeArgument ->
            val argType =
                irTypeArgument as? IrTypeProjection ?: throw IllegalStateException("Star projection in immediate argument for supertype")
            typeArguments[index] = argType.type
        }
    }

    // returns offset in serializable properties array
    private fun IrBlockBodyBuilder.generateSuperSerializableCall(
        superClass: IrClass,
        allValueParameters: List<IrValueParameter>,
        propertiesStart: Int
    ) {
        check(superClass.shouldHaveGeneratedMethods())
        val superCtorRef = superClass.findSerializableSyntheticConstructor()
            ?: error("Class serializable internally should have special constructor with marker")
        val superProperties = serializablePropertiesForIrBackend(superClass).serializableProperties
        val superSlots = superProperties.bitMaskSlotCount()
        val arguments = allValueParameters.subList(0, superSlots) +
                allValueParameters.subList(propertiesStart, propertiesStart + superProperties.size) +
                allValueParameters.last() // SerializationConstructorMarker
        +irDelegatingConstructorCall(superCtorRef) {
            // `this.`, because `arguments` is also a local variable.
            this.arguments.assignFrom(arguments) { irGetValue(it.symbol) }
            insertTypeArgumentsForSuperClass(superClass)
        }
    }

    fun generateWriteSelfMethod(methodDescriptor: IrSimpleFunction) {
        addFunctionBody(methodDescriptor) { writeSelfFunction ->
            val [objectToSerialize, localOutput, localSerialDesc] = writeSelfFunction.nonDispatchParameters
            val serializableProperties = properties.serializableProperties
            val kOutputClass = compilerContext.getClassFromRuntime(SerialEntityNames.STRUCTURE_ENCODER_CLASS)

            val propertyByParamReplacer: (IrValueParameterSymbol) -> IrExpression? =
                createPropertyByParamReplacer(irClass, serializableProperties, objectToSerialize)

            // Since writeSelf is a static method, we have to replace all references to this in property initializers
            val thisSymbol = irClass.thisReceiver!!.symbol
            val initializerAdapter: (IrExpressionBody) -> IrExpression =
                createInitializerAdapter(irClass, propertyByParamReplacer, thisSymbol to { irGetValue(objectToSerialize.symbol) })

            // Compute offset of properties in superclass
            var ignoreIndexTo = -1
            val superClass = irClass.getSuperClassOrAny()
            if (superClass.isInternalSerializable) {
                ignoreIndexTo = serializablePropertiesForIrBackend(superClass).serializableProperties.size

                // call super.writeSelf
                var superWriteSelfF = superClass.findWriteSelfMethod()

                if (superWriteSelfF != null) {
                    // Workaround for incorrect DeserializedClassDescriptor on JVM (see MemberDeserializer#getDispatchReceiverParameter):
                    // Because Kotlin does not have static functions, descriptors from other modules are deserialized with dispatch receiver,
                    // even if they were created without it
                    if (superWriteSelfF.dispatchReceiverParameter != null) {
                        superWriteSelfF = compilerContext.copiedStaticWriteSelf.getOrPut(superWriteSelfF) {
                            superWriteSelfF.deepCopyWithSymbols(initialParent = superClass).also {
                                it.parameters = it.nonDispatchParameters
                            }
                        }
                    }

                    val args = mutableListOf<IrExpression>(irGetValue(objectToSerialize.symbol), irGetValue(localOutput.symbol), irGetValue(localSerialDesc.symbol))

                    val typeArgsForParent =
                        (irClass.superTypes.single { it.classOrNull?.owner?.isInternalSerializable == true } as? IrSimpleType)?.arguments.orEmpty()
                    val parentWriteSelfSerializers = typeArgsForParent.map { arg ->
                        val genericIdx = irClass.defaultType.arguments.indexOf(arg).let { if (it == -1) null else it }
                        val serial = findTypeSerializerOrContext(compilerContext, arg.typeOrNull!!)
                        serializerInstance(
                            serial,
                            compilerContext,
                            arg.typeOrNull!!,
                            genericIdx,
                            irClass
                        ) { it, _ ->
                            irGetValue(writeSelfFunction.nonDispatchParameters[3 + it].symbol)
                        }!!
                    }
                    +irInvoke(superWriteSelfF.symbol, args + parentWriteSelfSerializers, typeArgsForParent.map { it.typeOrNull!! })
                }
            }

            val cachedChildSerializerByIndex = createCacheableChildSerializersFactory(
                cachedChildSerializersProperty,
                cachedChildSerializers.map { it != null }
            ) { irClass.companionObject()!! }

            serializeAllProperties(
                serializableProperties, objectToSerialize,
                localOutput, localSerialDesc, kOutputClass,
                ignoreIndexTo, initializerAdapter, cachedChildSerializerByIndex,
            ) { it, _ ->
                irGetValue(writeSelfFunction.nonDispatchParameters[3 + it].symbol)
            }
        }
    }

    fun generate() {
        generateSyntheticInternalConstructor()
        generateSyntheticMethods()
    }

    private fun generateSyntheticInternalConstructor() {
        val serializerDescriptor = irClass.findSerializerForGeneratedMethods(compilerContext)?.owner ?: return
        if (irClass.shouldHaveSpecificSyntheticMethods(isJvm = compilerContext.platform.isJvm()) { serializerDescriptor.findPluginGeneratedMethod(LOAD) }) {
            val constrDesc = irClass.constructors.find(IrConstructor::isSerializationCtor) ?: return
            generateInternalConstructor(constrDesc)
        }
    }

    private fun generateSyntheticMethods() {
        val serializerDescriptor = irClass.findSerializerForGeneratedMethods(compilerContext)?.owner ?: return
        if (irClass.shouldHaveSpecificSyntheticMethods(isJvm = compilerContext.platform.isJvm()) { serializerDescriptor.findPluginGeneratedMethod(SAVE) }) {
            val func = irClass.findWriteSelfMethod() ?: return
            func.origin = SERIALIZATION_PLUGIN_ORIGIN
            generateWriteSelfMethod(func)
        }
    }


    companion object {
        fun generate(
            irClass: IrClass,
            context: SerializationPluginContext,
        ) {
            if (irClass.shouldHaveGeneratedMethods()) {
                SerializableIrGenerator(irClass, context).generate()
                irClass.patchDeclarationParents(irClass.parent)
            } else {
                val serializableAnnotationIsUseless = with(irClass) {
                    hasSerializableOrMetaAnnotationWithoutArgs() && !isInternalSerializable && !hasCompanionObjectAsSerializer && kind != ClassKind.ENUM_CLASS && !isSealedSerializableInterface
                }
                if (serializableAnnotationIsUseless)
                    error(
                        "@Serializable annotation on $irClass would be ignored because it is impossible to serialize it automatically. " +
                                "Provide serializer manually via e.g. companion object"
                    )
            }
        }
    }
}
