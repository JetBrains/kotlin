/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCodegen
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.serializableAnnotationIsUseless
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MISSING_FIELD_EXC

class SerializableIrGenerator(
    val irClass: IrClass,
    override val compilerContext: SerializationPluginContext,
    bindingContext: BindingContext
) : SerializableCodegen(irClass.descriptor, bindingContext), IrBuilderExtension {



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
                    irThrow(irInvoke(null, exceptionCtorRef, irString(prop.name), typeHint = exceptionType))
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