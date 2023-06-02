/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.lower.isJvmOptimizableDelegate
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.getOrPutNullable
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.CACHED_DESCRIPTOR_FIELD_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.LOAD
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SAVE
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESC_FIELD

class SerializableIrGenerator(
    val irClass: IrClass,
    compilerContext: SerializationPluginContext
) : BaseIrGenerator(irClass, compilerContext) {

    protected val properties = serializablePropertiesForIrBackend(irClass)

    private val serialDescriptorClass = compilerContext.referenceClass(
        SerializationRuntimeClassIds.descriptorClassId
    )!!.owner

    private val serialDescriptorImplClass = compilerContext.referenceClass(
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

            val serialDescs = serializableProperties.map { it.ir }.toSet()

            val propertyByParamReplacer: (ValueParameterDescriptor) -> IrExpression? =
                createPropertyByParamReplacer(irClass, serializableProperties, thiz)

            val initializerAdapter: (IrExpressionBody) -> IrExpression = createInitializerAdapter(irClass, propertyByParamReplacer)


            var current: IrProperty? = null
            val statementsAfterSerializableProperty: MutableMap<IrProperty?, MutableList<IrStatement>> = mutableMapOf()
            irClass.declarations.asSequence().forEach {
                when {
                    // only properties with backing field
                    it is IrProperty && it.backingField != null -> {
                        if (it in serialDescs) {
                            current = it
                        } else if (it.backingField?.initializer != null &&
                            !(it.isJvmOptimizableDelegate() && compilerContext.platform.isJvm())
                        ) {
                            // skip transient lateinit or deferred properties (with null initializer) and optimized delegations
                            val expression = initializerAdapter(it.backingField!!.initializer!!)

                            statementsAfterSerializableProperty.getOrPutNullable(current, { mutableListOf() })
                                .add(irSetField(irGet(thiz), it.backingField!!, expression))
                        }
                    }
                    it is IrAnonymousInitializer -> {
                        val statements = it.body.deepCopyWithVariables().statements
                        statementsAfterSerializableProperty.getOrPutNullable(current, { mutableListOf() })
                            .addAll(statements)
                    }
                }
            }

            // Missing field exception parts
            val exceptionCtorRef =
                compilerContext.referenceConstructors(ClassId(SerializationPackages.packageFqName, Name.identifier(MISSING_FIELD_EXC)))
                    .single { it.owner.valueParameters.singleOrNull()?.type?.isString() == true }
            val exceptionType = exceptionCtorRef.owner.returnType

            val seenVarsOffset = serializableProperties.bitMaskSlotCount()
            val seenVars = (0 until seenVarsOffset).map { ctor.valueParameters[it] }


            val superClass = irClass.getSuperClassOrAny()
            var startPropOffset: Int = 0


            if (useFieldMissingOptimization() &&
                // for abstract classes fields MUST BE checked in child classes
                !irClass.isAbstractOrSealedSerializableClass
            ) {
                val getDescriptorExpr = if (irClass.isStaticSerializable) {
                    getStaticSerialDescriptorExpr()
                } else {
                    // synthetic constructor is created only for internally serializable classes - so companion definitely exists
                    val companionObject = irClass.companionObject()!!
                    getParametrizedSerialDescriptorExpr(companionObject, createCachedDescriptorProperty(companionObject))
                }
                generateGoldenMaskCheck(seenVars, properties, getDescriptorExpr)
            }
            when {
                superClass.symbol == compilerContext.irBuiltIns.anyClass -> generateAnySuperConstructorCall(toBuilder = this@addFunctionBody)
                superClass.isInternalSerializable -> {
                    startPropOffset = generateSuperSerializableCall(superClass, ctor.valueParameters, seenVarsOffset)
                }
                else -> generateSuperNonSerializableCall(superClass)
            }

            statementsAfterSerializableProperty[null]?.forEach { +it }
            for (index in startPropOffset until serializableProperties.size) {
                val prop = serializableProperties[index]
                val paramRef = ctor.valueParameters[index + seenVarsOffset]
                // Assign this.a = a in else branch
                // Set field directly w/o setter to match behavior of old backend plugin
                val backingFieldToAssign = prop.ir.backingField!!
                val assignParamExpr = irSetField(irGet(thiz), backingFieldToAssign, irGet(paramRef))

                val ifNotSeenExpr: IrExpression = if (prop.optional) {
                    val initializerBody =
                        requireNotNull(initializerAdapter(prop.ir.backingField?.initializer!!)) { "Optional value without an initializer" } // todo: filter abstract here
                    irSetField(irGet(thiz), backingFieldToAssign, initializerBody)
                } else {
                    // property required
                    if (useFieldMissingOptimization()) {
                        // field definitely not empty as it's checked before - no need another IF, only assign property from param
                        +assignParamExpr
                        statementsAfterSerializableProperty[prop.ir]?.forEach { +it }
                        continue
                    } else {
                        irThrow(irInvoke(null, exceptionCtorRef, irString(prop.name), typeHint = exceptionType))
                    }
                }

                val propNotSeenTest =
                    irEquals(
                        irInt(0),
                        irBinOp(
                            OperatorNameConventions.AND,
                            irGet(seenVars[bitMaskSlotAt(index)]),
                            irInt(1 shl (index % 32))
                        )
                    )

                +irIfThenElse(compilerContext.irBuiltIns.unitType, propNotSeenTest, ifNotSeenExpr, assignParamExpr)

                statementsAfterSerializableProperty[prop.ir]?.forEach { +it }
            }

            // Handle function-intialized interface delegates
            irClass.declarations
                .filterIsInstance<IrField>()
                .filter { it.origin == IrDeclarationOrigin.DELEGATE }
                .forEach {
                    val receiver = if (!it.isStatic) irGet(thiz) else null
                    +irSetField(
                        receiver,
                        it,
                        initializerAdapter(it.initializer!!),
                        IrStatementOrigin.INITIALIZE_FIELD
                    )
                }
        }

    private fun IrBlockBodyBuilder.getStaticSerialDescriptorExpr(): IrExpression {
        val serializerIrClass = irClass.classSerializer(compilerContext)!!.owner
        // internally generated serializer always declared inside serializable class

        val serialDescriptorGetter =
            serializerIrClass.getPropertyGetter(SERIAL_DESC_FIELD)!!.owner
        return irGet(
            serialDescriptorGetter.returnType,
            irGetObject(serializerIrClass),
            serialDescriptorGetter.symbol
        )
    }

    private fun IrBlockBodyBuilder.getParametrizedSerialDescriptorExpr(companionObject: IrClass, property: IrProperty): IrExpression {
        return irGetField(irGetObject(companionObject), property.backingField!!)
    }

    private fun createCachedDescriptorProperty(companionObject: IrClass): IrProperty {
        val serialDescIrType = serialDescriptorClass.defaultType

        return companionObject.addValPropertyWithJvmField(serialDescIrType, CACHED_DESCRIPTOR_FIELD_NAME, DescriptorVisibilities.PUBLIC) {
            val serialDescVar = irTemporary(
                getInstantiateDescriptorExpr(),
                nameHint = "serialDesc"
            )
            for (property in properties.serializableProperties) {
                +getAddElementToDescriptorExpr(property, serialDescVar)
            }
            +irGet(serialDescVar)
        }
    }

    private fun IrBlockBodyBuilder.getInstantiateDescriptorExpr(): IrExpression {
        val classConstructors = serialDescriptorImplClass.constructors
        val serialClassDescImplCtor = classConstructors.single { it.isPrimary }.symbol
        return irInvoke(
            null, serialClassDescImplCtor,
            irString(irClass.serialName()), irNull(), irInt(properties.serializableProperties.size)
        )
    }

    private fun IrBlockBodyBuilder.getAddElementToDescriptorExpr(
        property: IrSerializableProperty,
        serialDescVar: IrVariable
    ): IrExpression {
        return irInvoke(
            irGet(serialDescVar),
            addElementFun,
            irString(property.name),
            irBoolean(property.optional),
            typeHint = compilerContext.irBuiltIns.unitType
        )
    }

    private fun IrBlockBodyBuilder.generateSuperNonSerializableCall(superClass: IrClass) {
        val ctorRef = superClass.declarations.filterIsInstance<IrConstructor>().singleOrNull { it.valueParameters.isEmpty() }
            ?: error("Non-serializable parent of serializable $irClass must have no arg constructor")


        val call = IrDelegatingConstructorCallImpl.fromSymbolOwner(
            startOffset,
            endOffset,
            compilerContext.irBuiltIns.unitType,
            ctorRef.symbol
        )
        call.insertTypeArgumentsForSuperClass(superClass)
        +call
    }

    private fun IrDelegatingConstructorCallImpl.insertTypeArgumentsForSuperClass(superClass: IrClass) {
        val superTypeCallArguments = (irClass.superTypes.find { it.classOrNull == superClass.symbol } as IrSimpleType?)?.arguments
        superTypeCallArguments?.forEachIndexed { index, irTypeArgument ->
            val argType =
                irTypeArgument as? IrTypeProjection ?: throw IllegalStateException("Star projection in immediate argument for supertype")
            putTypeArgument(index, argType.type)
        }
    }

    // returns offset in serializable properties array
    private fun IrBlockBodyBuilder.generateSuperSerializableCall(
        superClass: IrClass,
        allValueParameters: List<IrValueParameter>,
        propertiesStart: Int
    ): Int {
        check(superClass.isInternalSerializable)
        val superCtorRef = superClass.findSerializableSyntheticConstructor()
            ?: error("Class serializable internally should have special constructor with marker")
        val superProperties = serializablePropertiesForIrBackend(superClass).serializableProperties
        val superSlots = superProperties.bitMaskSlotCount()
        val arguments = allValueParameters.subList(0, superSlots) +
                allValueParameters.subList(propertiesStart, propertiesStart + superProperties.size) +
                allValueParameters.last() // SerializationConstructorMarker
        val call = IrDelegatingConstructorCallImpl.fromSymbolOwner(
            startOffset,
            endOffset,
            compilerContext.irBuiltIns.unitType,
            superCtorRef
        )
        arguments.forEachIndexed { index, parameter -> call.putValueArgument(index, irGet(parameter)) }
        call.insertTypeArgumentsForSuperClass(superClass)
        +call
        return superProperties.size
    }

    fun generateWriteSelfMethod(methodDescriptor: IrSimpleFunction) {
        addFunctionBody(methodDescriptor) { writeSelfFunction ->
            val objectToSerialize = writeSelfFunction.valueParameters[0]
            val localOutput = writeSelfFunction.valueParameters[1]
            val localSerialDesc = writeSelfFunction.valueParameters[2]
            val serializableProperties = properties.serializableProperties
            val kOutputClass = compilerContext.getClassFromRuntime(SerialEntityNames.STRUCTURE_ENCODER_CLASS)

            val propertyByParamReplacer: (ValueParameterDescriptor) -> IrExpression? =
                createPropertyByParamReplacer(irClass, serializableProperties, objectToSerialize)

            // Since writeSelf is a static method, we have to replace all references to this in property initializers
            val thisSymbol = irClass.thisReceiver!!.symbol
            val initializerAdapter: (IrExpressionBody) -> IrExpression =
                createInitializerAdapter(irClass, propertyByParamReplacer, thisSymbol to { irGet(objectToSerialize) })

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
                            superWriteSelfF!!.deepCopyWithSymbols(initialParent = superClass).also { it.dispatchReceiverParameter = null }
                        }
                    }

                    val args = mutableListOf<IrExpression>(irGet(objectToSerialize), irGet(localOutput), irGet(localSerialDesc))

                    val typeArgsForParent =
                        (irClass.superTypes.single { it.classOrNull?.owner?.isInternalSerializable == true } as? IrSimpleType)?.arguments.orEmpty()
                    val parentWriteSelfSerializers = typeArgsForParent.map { arg ->
                        val genericIdx = irClass.defaultType.arguments.indexOf(arg).let { if (it == -1) null else it }
                        val serial = findTypeSerializerOrContext(compilerContext, arg.typeOrNull!!)
                        serializerInstance(
                            serial,
                            compilerContext,
                            arg.typeOrNull!!,
                            genericIdx
                        ) { it, _ ->
                            irGet(writeSelfFunction.valueParameters[3 + it])
                        }!!
                    }
                    +irInvoke(null, superWriteSelfF.symbol, typeArgsForParent.map { it.typeOrNull!! }, args + parentWriteSelfSerializers)
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
                irGet(writeSelfFunction.valueParameters[3 + it])
            }
        }
    }

    fun generate() {
        generateSyntheticInternalConstructor()
        generateSyntheticMethods()
    }

    private fun generateSyntheticInternalConstructor() {
        val serializerDescriptor = irClass.classSerializer(compilerContext)?.owner ?: return
        if (irClass.shouldHaveSpecificSyntheticMethods { serializerDescriptor.findPluginGeneratedMethod(LOAD, compilerContext.afterK2) }) {
            val constrDesc = irClass.constructors.find(IrConstructor::isSerializationCtor) ?: return
            generateInternalConstructor(constrDesc)
        }
    }

    private fun generateSyntheticMethods() {
        val serializerDescriptor = irClass.classSerializer(compilerContext)?.owner ?: return
        if (irClass.shouldHaveSpecificSyntheticMethods { serializerDescriptor.findPluginGeneratedMethod(SAVE, compilerContext.afterK2) }) {
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
            if (irClass.isInternalSerializable) {
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
