/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.getOrPutNullable
import org.jetbrains.kotlinx.serialization.compiler.backend.common.*
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.serializableAnnotationIsUseless
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.CACHED_DESCRIPTOR_FIELD_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESC_FIELD

class SerializableIrGenerator(
    val irClass: IrClass,
    override val compilerContext: SerializationPluginContext,
    bindingContext: BindingContext
) : SerializableCodegen(irClass.descriptor, bindingContext), IrBuilderExtension {

    private val serialDescClass: ClassDescriptor = serializableDescriptor.module
        .getClassFromSerializationDescriptorsPackage(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS)

    private val serialDescImplClass: ClassDescriptor = serializableDescriptor
        .getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL)

    private val addElementFun = serialDescImplClass.referenceFunctionSymbol(CallingConventions.addElement)

    private fun IrClass.hasSerializableAnnotationWithoutArgs(): Boolean {
        val annot = getAnnotation(SerializationAnnotations.serializableAnnotationFqName) ?: return false

        for (i in 0 until annot.valueArgumentsCount) {
            if (annot.getValueArgument(i) != null) return false
        }

        return true
    }

    private val IrClass.isInternalSerializable: Boolean get() = kind == ClassKind.CLASS && hasSerializableAnnotationWithoutArgs()

    override fun generateInternalConstructor(constructorDescriptor: ClassConstructorDescriptor) =
        irClass.contributeConstructor(constructorDescriptor) { ctor ->
            val thiz = irClass.thisReceiver!!
            val serializableProperties = properties.serializableProperties

            val serialDescs = serializableProperties.map { it.descriptor }.toSet()

            val propertyByParamReplacer: (ValueParameterDescriptor) -> IrExpression? =
                createPropertyByParamReplacer(irClass, serializableProperties, thiz, bindingContext)

            val initializerAdapter: (IrExpressionBody) -> IrExpression = createInitializerAdapter(irClass, propertyByParamReplacer)


            var current: PropertyDescriptor? = null
            val statementsAfterSerializableProperty: MutableMap<PropertyDescriptor?, MutableList<IrStatement>> = mutableMapOf()
            irClass.declarations.asSequence().forEach {
                when {
                    // only properties with backing field
                    it is IrProperty && it.backingField != null -> {
                        if (it.descriptor in serialDescs) {
                            current = it.descriptor
                        } else if (it.backingField?.initializer != null) {
                            // skip transient lateinit or deferred properties (with null initializer)
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
            val exceptionFqn = getSerializationPackageFqn(MISSING_FIELD_EXC)
            val exceptionCtorRef = compilerContext.referenceConstructors(exceptionFqn)
                .single { it.owner.valueParameters.singleOrNull()?.type?.isString() == true }
            val exceptionType = exceptionCtorRef.owner.returnType

            val seenVarsOffset = serializableProperties.bitMaskSlotCount()
            val seenVars = (0 until seenVarsOffset).map { ctor.valueParameters[it] }


            val superClass = irClass.getSuperClassOrAny()
            var startPropOffset: Int = 0


            if (useFieldMissingOptimization() &&
                // for abstract classes fields MUST BE checked in child classes
                !serializableDescriptor.isAbstractOrSealedSerializableClass()
            ) {
                val getDescriptorExpr = if (serializableDescriptor.isStaticSerializable) {
                    getStaticSerialDescriptorExpr()
                } else {
                    // synthetic constructor is created only for internally serializable classes - so companion definitely exists
                    val companionObject = irClass.companionObject()!!
                    getParametrizedSerialDescriptorExpr(companionObject, createCachedDescriptorProperty(companionObject))
                }
                generateGoldenMaskCheck(seenVars, properties, getDescriptorExpr)
            }
            when {
                superClass.symbol == compilerContext.irBuiltIns.anyClass -> generateAnySuperConstructorCall(toBuilder = this@contributeConstructor)
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
                val backingFieldToAssign = prop.getIrPropertyFrom(irClass).backingField!!
                val assignParamExpr = irSetField(irGet(thiz), backingFieldToAssign, irGet(paramRef))

                val ifNotSeenExpr: IrExpression = if (prop.optional) {
                    val initializerBody =
                        requireNotNull(initializerAdapter(prop.irField.initializer!!)) { "Optional value without an initializer" } // todo: filter abstract here
                    irSetField(irGet(thiz), backingFieldToAssign, initializerBody)
                } else {
                    // property required
                    if (useFieldMissingOptimization()) {
                        // field definitely not empty as it's checked before - no need another IF, only assign property from param
                        +assignParamExpr
                        statementsAfterSerializableProperty[prop.descriptor]?.forEach { +it }
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

                statementsAfterSerializableProperty[prop.descriptor]?.forEach { +it }
            }
        }

    private fun IrBlockBodyBuilder.getStaticSerialDescriptorExpr(): IrExpression {
        val serializer = serializableDescriptor.classSerializer!!
        val serialDescriptorGetter = compilerContext.referenceClass(serializer.fqNameSafe)?.getPropertyGetter(SERIAL_DESC_FIELD)
            ?: throw Exception("No class with name ${serializer.fqNameSafe}")
        return irGet(
            serialDescriptorGetter.owner.returnType,
            irGetObject(serializer),
            serialDescriptorGetter.owner.symbol
        )
    }

    private fun IrBlockBodyBuilder.getParametrizedSerialDescriptorExpr(companionObject: IrClass, property: IrProperty): IrExpression {
        return irGetField(irGetObject(companionObject), property.backingField!!)
    }

    private fun IrBlockBodyBuilder.createCachedDescriptorProperty(companionObject: IrClass): IrProperty {
        val serialDescIrType = serialDescClass.defaultType.toIrType()

        return createCompanionValProperty(companionObject, serialDescIrType, CACHED_DESCRIPTOR_FIELD_NAME) {
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
        val classConstructors = compilerContext.referenceConstructors(serialDescImplClass.fqNameSafe)
        val serialClassDescImplCtor = classConstructors.single { it.owner.isPrimary }
        return irInvoke(
            null, serialClassDescImplCtor,
            irString(serializableDescriptor.serialName()), irNull(), irInt(properties.serializableProperties.size)
        )
    }

    private fun IrBlockBodyBuilder.getAddElementToDescriptorExpr(
        property: SerializableProperty,
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
            ?: error("Non-serializable parent of serializable $serializableDescriptor must have no arg constructor")


        val call = IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
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
        val superCtorRef = serializableSyntheticConstructor(superClass)
        val superProperties = bindingContext.serializablePropertiesFor(superClass.descriptor).serializableProperties
        val superSlots = superProperties.bitMaskSlotCount()
        val arguments = allValueParameters.subList(0, superSlots) +
                    allValueParameters.subList(propertiesStart, propertiesStart + superProperties.size) +
                    allValueParameters.last() // SerializationConstructorMarker
        val call = IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
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

    override fun generateWriteSelfMethod(methodDescriptor: FunctionDescriptor) {
        irClass.contributeFunction(methodDescriptor, ignoreWhenMissing = true) { writeSelfFunction ->
            val objectToSerialize = writeSelfFunction.valueParameters[0]
            val localOutput = writeSelfFunction.valueParameters[1]
            val localSerialDesc = writeSelfFunction.valueParameters[2]
            val serializableProperties = properties.serializableProperties
            val kOutputClass = serializableDescriptor.getClassFromSerializationPackage(SerialEntityNames.STRUCTURE_ENCODER_CLASS)

            val propertyByParamReplacer: (ValueParameterDescriptor) -> IrExpression? =
                createPropertyByParamReplacer(irClass, serializableProperties, objectToSerialize, bindingContext)

            // Since writeSelf is a static method, we have to replace all references to this in property initializers
            val thisSymbol = irClass.thisReceiver!!.symbol
            val initializerAdapter: (IrExpressionBody) -> IrExpression = createInitializerAdapter(irClass, propertyByParamReplacer, thisSymbol to { irGet(objectToSerialize) })

            // Compute offset of properties in superclass
            var ignoreIndexTo = -1
            val superClass = irClass.getSuperClassOrAny()
            if (superClass.descriptor.isInternalSerializable) {
                ignoreIndexTo = bindingContext.serializablePropertiesFor(superClass.descriptor).size

                // call super.writeSelf
                val superWriteSelfF = superClass.findWriteSelfMethod()
                if (superWriteSelfF != null) {
                    val args = mutableListOf<IrExpression>(irGet(objectToSerialize), irGet(localOutput), irGet(localSerialDesc))

                    val typeArgsForParent = serializableDescriptor.typeConstructor.supertypes.single { it.toClassDescriptor?.isInternalSerializable == true }.arguments
                    val parentWriteSelfSerializers = typeArgsForParent.map { arg ->
                        val genericIdx = serializableDescriptor.defaultType.arguments.indexOf(arg).let { if (it == -1) null else it }
                        val serial = findTypeSerializerOrContext(serializableDescriptor.module, arg.type)
                        serializerInstance(
                            this@SerializableIrGenerator,
                            serial,
                            serializableDescriptor.module,
                            arg.type,
                            genericIdx
                        ) { it, _ ->
                            irGet(writeSelfFunction.valueParameters[3 + it])
                        }!!
                    }
                    +irInvoke(null, superWriteSelfF.symbol, typeArgsForParent.map { it.type.toIrType() }, args + parentWriteSelfSerializers)
                }
            }

            serializeAllProperties(
                this@SerializableIrGenerator, irClass, serializableProperties,
                objectToSerialize, localOutput, localSerialDesc,
                kOutputClass, ignoreIndexTo, initializerAdapter
            ) { it, _ ->
                irGet(writeSelfFunction.valueParameters[3 + it])
            }
        }
    }

    companion object {
        fun generate(
            irClass: IrClass,
            context: SerializationPluginContext,
            bindingContext: BindingContext
        ) {
            val serializableClass = irClass.descriptor

            if (serializableClass.isInternalSerializable) {
                SerializableIrGenerator(irClass, context, bindingContext).generate()
                irClass.patchDeclarationParents(irClass.parent)
            } else if (serializableClass.serializableAnnotationIsUseless) {
                throw CompilationException(
                    "@Serializable annotation on $serializableClass would be ignored because it is impossible to serialize it automatically. " +
                            "Provide serializer manually via e.g. companion object", null, serializableClass.findPsi()
                )
            }
        }
    }
}
