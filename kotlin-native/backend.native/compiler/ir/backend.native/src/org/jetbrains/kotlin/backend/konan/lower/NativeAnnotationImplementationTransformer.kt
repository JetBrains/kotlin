/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.AnnotationImplementationTransformer
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.isFinalClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl


internal class NativeAnnotationImplementationTransformer(context: Context, irFile: IrFile) :
        AnnotationImplementationTransformer(context, irFile) {

    private val arrayContentEqualsMap = context.ir.symbols.arraysContentEquals

    override fun getArrayContentEqualsSymbol(type: IrType) =
            when {
                type.isPrimitiveArray() -> arrayContentEqualsMap[type]
                else -> arrayContentEqualsMap.entries.singleOrNull { (k, _) -> k.isArray() }?.value
            } ?: error("Can't find an Arrays.contentEquals method for array type ${type.render()}")

    override fun IrClass.platformSetup() {
        visibility = DescriptorVisibilities.PRIVATE
        parent = irFile!!
    }

    override fun implementAnnotationPropertiesAndConstructor(implClass: IrClass, annotationClass: IrClass, generatedConstructor: IrConstructor) {
        require(!annotationClass.isFinalClass) { "Annotation class ${annotationClass.kotlinFqName} shouldn't be final" }
        val properties = annotationClass.getAnnotationProperties()
        properties.forEach { property ->
            val propType = property.getter!!.returnType
            val propName = property.name
            val parameter = generatedConstructor.addValueParameter(propName.asString(), propType)
            // VALUE_FROM_PARAMETER
            val originalParameter = ((property.backingField?.initializer?.expression as? IrGetValue)?.symbol?.owner as? IrValueParameter)
            if (originalParameter?.defaultValue != null) {
                parameter.defaultValue = originalParameter.defaultValue!!.deepCopyWithVariables().also { it.transformChildrenVoid() }
            }
        }

        generatedConstructor.body = context.irFactory.createBlockBody(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, listOf(
                IrDelegatingConstructorCallImpl(
                        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.unitType, annotationClass.constructors.single().symbol,
                        typeArgumentsCount = 0, valueArgumentsCount = generatedConstructor.valueParameters.size
                ).apply {
                    generatedConstructor.valueParameters.forEach {
                        putValueArgument(it.index, IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, it.symbol))
                    }
                }
        ))
    }

    override val forbidDirectFieldAccessInMethods = true
}