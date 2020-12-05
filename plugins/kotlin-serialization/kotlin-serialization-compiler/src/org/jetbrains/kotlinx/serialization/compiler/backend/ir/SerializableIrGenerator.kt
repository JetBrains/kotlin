/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.serializableAnnotationIsUseless
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ARRAY_MASK_FIELD_MISSING_FUNC_FQ
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESC_FIELD
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SINGLE_MASK_FIELD_MISSING_FUNC_FQ
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.initializedDescriptorFieldName

class SerializableIrGenerator(
    val irClass: IrClass,
    override val compilerContext: SerializationPluginContext,
    bindingContext: BindingContext
) : SerializableCodegen(irClass.descriptor, bindingContext), IrBuilderExtension {

    private val descriptorGenerationFunctionName = "createInitializedDescriptor"

    private val serialDescClass: ClassDescriptor = serializableDescriptor.module
        .getClassFromSerializationDescriptorsPackage(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS)

    private val serialDescImplClass: ClassDescriptor = serializableDescriptor
        .getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL)

    private val addElementFun = serialDescImplClass.findFunctionSymbol(CallingConventions.addElement)

    val throwMissedFieldExceptionFunc =
        if (useFieldMissingOptimization) compilerContext.referenceFunctions(SINGLE_MASK_FIELD_MISSING_FUNC_FQ).single() else null
    val throwMissedFieldExceptionArrayFunc =
        if (useFieldMissingOptimization) compilerContext.referenceFunctions(ARRAY_MASK_FIELD_MISSING_FUNC_FQ).single() else null

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
            val transformFieldInitializer = buildInitializersRemapping(irClass)

            // Missing field exception parts
            val exceptionFqn = getSerializationPackageFqn(MISSING_FIELD_EXC)
            val exceptionCtorRef = compilerContext.referenceConstructors(exceptionFqn)
                .single { it.owner.valueParameters.singleOrNull()?.type?.isString() == true }
            val exceptionType = exceptionCtorRef.owner.returnType

            val serializableProperties = properties.serializableProperties
            val seenVarsOffset = serializableProperties.bitMaskSlotCount()
            val seenVars = (0 until seenVarsOffset).map { ctor.valueParameters[it] }

            val thiz = irClass.thisReceiver!!
            val superClass = irClass.getSuperClassOrAny()
            var startPropOffset: Int = 0

            if (useFieldMissingOptimization) {
                generateOptimizedGoldenMaskCheck(seenVars)
            }
            when {
                superClass.symbol == compilerContext.irBuiltIns.anyClass -> generateAnySuperConstructorCall(toBuilder = this@contributeConstructor)
                superClass.isInternalSerializable -> {
                    startPropOffset = generateSuperSerializableCall(superClass, ctor.valueParameters, seenVarsOffset)
                }
                else -> generateSuperNonSerializableCall(superClass)
            }

            for (index in startPropOffset until serializableProperties.size) {
                val prop = serializableProperties[index]
                val paramRef = ctor.valueParameters[index + seenVarsOffset]
                // assign this.a = a in else branch
                val assignParamExpr = setProperty(irGet(thiz), prop.irProp, irGet(paramRef))

                val ifNotSeenExpr: IrExpression = if (prop.optional) {
                    val initializerBody =
                        requireNotNull(transformFieldInitializer(prop.irField)) { "Optional value without an initializer" } // todo: filter abstract here
                    setProperty(irGet(thiz), prop.irProp, initializerBody)
                } else {
                    // property required
                    if (useFieldMissingOptimization) {
                        // field definitely not empty as it's checked before - no need another IF, only assign property from param
                        +assignParamExpr
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
            }

            // remaining initializers of variables
            val serialDescs = serializableProperties.map { it.descriptor }.toSet()
            irClass.declarations.asSequence()
                .filterIsInstance<IrProperty>()
                .filter { it.descriptor !in serialDescs }
                .filter { it.backingField != null }
                .mapNotNull { prop -> transformFieldInitializer(prop.backingField!!)?.let { prop to it } }
                .forEach { (prop, expr) -> +irSetField(irGet(thiz), prop.backingField!!, expr) }

            // init blocks
            irClass.declarations.asSequence()
                .filterIsInstance<IrAnonymousInitializer>()
                .forEach { initializer ->
                    initializer.body.deepCopyWithVariables().statements.forEach { +it }
                }
        }

    private fun IrBlockBodyBuilder.generateOptimizedGoldenMaskCheck(seenVars: List<IrValueParameter>) {
        if (serializableDescriptor.isAbstractSerializableClass() || serializableDescriptor.isSealedSerializableClass()) {
            // for abstract classes fields MUST BE checked in child classes
            return
        }

        val fieldsMissedTest: IrExpression
        val throwErrorExpr: IrExpression

        val maskSlotCount = seenVars.size
        if (maskSlotCount == 1) {
            val goldenMask = getGoldenMask()

            throwErrorExpr = irInvoke(
                null,
                throwMissedFieldExceptionFunc!!,
                irGet(seenVars[0]),
                irInt(goldenMask),
                getSerialDescriptorExpr(),
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
            val goldenMaskList = getGoldenMaskList()

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
                    getSerialDescriptorExpr(),
                    typeHint = compilerContext.irBuiltIns.unitType
                )
            }
        }

        +irIfThen(compilerContext.irBuiltIns.unitType, fieldsMissedTest, throwErrorExpr)
    }

    private fun IrBlockBodyBuilder.getSerialDescriptorExpr(): IrExpression {
        return if (serializableDescriptor.shouldHaveGeneratedSerializer && staticDescriptor) {
            val serializer = serializableDescriptor.classSerializer!!
            val serialDescriptorGetter = compilerContext.referenceClass(serializer.fqNameSafe)!!.getPropertyGetter(SERIAL_DESC_FIELD)!!
            irGet(
                serialDescriptorGetter.owner.returnType,
                irGetObject(serializer),
                serialDescriptorGetter.owner.symbol
            )
        } else {
            irGetField(null, generateStaticDescriptorField())
        }
    }

    private fun IrBlockBodyBuilder.generateStaticDescriptorField(): IrField {
        val serialDescItType = serialDescClass.defaultType.toIrType()

        val function = irClass.createInlinedFunction(
            Name.identifier(descriptorGenerationFunctionName),
            DescriptorVisibilities.PRIVATE,
            SERIALIZABLE_PLUGIN_ORIGIN,
            serialDescItType
        ) {
            val serialDescVar = irTemporary(
                getInstantiateDescriptorExpr(),
                nameHint = "serialDesc"
            )
            for (property in properties.serializableProperties) {
                +getAddElementToDescriptorExpr(property, serialDescVar)
            }
            +irReturn(irGet(serialDescVar))
        }

        return irClass.addField {
            name = Name.identifier(initializedDescriptorFieldName)
            visibility = DescriptorVisibilities.PRIVATE
            origin = SERIALIZABLE_PLUGIN_ORIGIN
            isFinal = true
            isStatic = true
            type = serialDescItType
        }.apply { initializer = irClass.factory.createExpressionBody(irCall(function)) }
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

    private inline fun ClassDescriptor.findFunctionSymbol(
        functionName: String,
        predicate: (IrSimpleFunction) -> Boolean = { true }
    ): IrFunctionSymbol {
        val irClass = compilerContext.referenceClass(fqNameSafe)?.owner ?: error("Couldn't load class $this")
        val simpleFunctions = irClass.declarations.filterIsInstance<IrSimpleFunction>()

        return simpleFunctions.filter { it.name.asString() == functionName }.single { predicate(it) }.symbol
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
        // no-op
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
            }
            else if (serializableClass.serializableAnnotationIsUseless) {
                throw CompilationException(
                    "@Serializable annotation on $serializableClass would be ignored because it is impossible to serialize it automatically. " +
                            "Provide serializer manually via e.g. companion object", null, serializableClass.findPsi()
                )
            }
        }
    }
}
