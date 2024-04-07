/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationLowering
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationTransformer
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.putArgument

internal class NativeAnnotationImplementationLowering(context: Context) : AnnotationImplementationLowering(
    { NativeAnnotationImplementationTransformer(context, it) }
)

private class NativeAnnotationImplementationTransformer(context: Context, irFile: IrFile) :
        AnnotationImplementationTransformer(context, irFile) {

    private val arrayContentEqualsMap = context.ir.symbols.arraysContentEquals

    override fun getArrayContentEqualsSymbol(type: IrType) =
            when {
                type.isPrimitiveArray() || type.isUnsignedArray() -> arrayContentEqualsMap[type]
                else -> arrayContentEqualsMap.entries.singleOrNull { (k, _) -> k.isArray() }?.value
            } ?: error("Can't find an Arrays.contentEquals method for array type ${type.render()}")

    override fun IrClass.platformSetup() {
        visibility = DescriptorVisibilities.PRIVATE
        parent = irFile!!
    }

    /**
     * When annotation is defined in another module, default values can be not available
     * during incremental compilation.
     *
     * In that case we need to delegate evaluating defaults to original class constructor.
     * The simplest way to do that - generate a constructor for each set of arguments used for
     * instantiating annotations, hope there shouldn't be too many of them in each module.
     */
    override fun chooseConstructor(implClass: IrClass, expression: IrConstructorCall) : IrConstructor {
        val existingValueArguments = (0 until expression.valueArgumentsCount)
                .filter { expression.getValueArgument(it) != null }
                .map { expression.symbol.owner.valueParameters[it].name }
                .toSet()
        return implClass.constructors.singleOrNull { cons ->
            cons.valueParameters.map { it.name }.toSet() == existingValueArguments
        } ?: implClass.addConstructor {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            visibility = DescriptorVisibilities.PUBLIC
        }.apply {
            expression.symbol.owner.valueParameters
                    .filter { it.name in existingValueArguments }
                    .forEach { parameter -> addValueParameter(parameter.name.asString(), parameter.type) }
            createConstructorBody(this, expression.symbol.owner)
        }
    }


    override fun implementAnnotationPropertiesAndConstructor(implClass: IrClass, annotationClass: IrClass, generatedConstructor: IrConstructor) {
        require(!annotationClass.isFinalClass) { "Annotation class ${annotationClass.kotlinFqName} shouldn't be final" }
        val properties = annotationClass.getAnnotationProperties()
        properties.forEach { property ->
            generatedConstructor.addValueParameter(property.name.asString(), property.getter!!.returnType)
        }
        createConstructorBody(generatedConstructor, annotationClass.primaryConstructor ?: error("Annotation class does not have primary constructor"))
    }

    private fun createConstructorBody(constructor: IrConstructor, delegate: IrConstructor) {
        /**
         * We need to delegate to base constructor, instead of calling primary with default values
         * as default values can be not available. {@see chooseConstructor} for details
         */
        constructor.body = context.irFactory.createBlockBody(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, listOf(
                IrDelegatingConstructorCallImpl(
                        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.unitType, delegate.symbol,
                        typeArgumentsCount = 0, valueArgumentsCount = delegate.valueParameters.size
                ).apply {
                    constructor.valueParameters.forEach { param ->
                        putArgument(delegate.valueParameters.single { it.name == param.name },
                                IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, param.symbol))
                    }
                }
        ))

    }

    override val forbidDirectFieldAccessInMethods = true
}