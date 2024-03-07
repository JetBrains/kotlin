/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithoutPatchingParents
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

fun IrBuilderWithScope.getProperty(receiver: IrExpression, property: IrProperty): IrExpression {
    return if (property.getter != null)
        irGet(property.getter!!.returnType, receiver, property.getter!!.symbol)
    else
        irGetField(receiver, property.backingField!!)
}

/*
  Create a function that creates `get property value expressions` for given corresponded constructor's param
    (constructor_params) -> get_property_value_expression
 */
fun IrBuilderWithScope.createPropertyByParamReplacer(
    irClass: IrClass,
    serialProperties: List<IrSerializableProperty>,
    instance: IrValueParameter
): (ValueParameterDescriptor) -> IrExpression? {
    fun IrSerializableProperty.irGet(): IrExpression {
        val ownerType = instance.symbol.owner.type
        return getProperty(
            irGet(
                type = ownerType,
                variable = instance.symbol
            ), ir
        )
    }

    val serialPropertiesMap = serialProperties.associateBy { it.ir }

    val transientPropertiesSet =
        irClass.declarations.asSequence()
            .filterIsInstance<IrProperty>()
            .filter { it.backingField != null }
            .filter { !serialPropertiesMap.containsKey(it) }
            .toSet()

    return { vpd ->
        val propertyDescriptor = irClass.properties.find { it.name == vpd.name }
        if (propertyDescriptor != null) {
            val value = serialPropertiesMap[propertyDescriptor]
            value?.irGet() ?: run {
                if (propertyDescriptor in transientPropertiesSet)
                    getProperty(
                        irGet(instance),
                        propertyDescriptor
                    )
                else null
            }
        } else {
            null
        }
    }
}

/*
    Creates an initializer adapter function that can replace IR expressions of getting constructor parameter value by some other expression.
    Also adapter may replace IR expression of getting `this` value by another expression.
     */
@OptIn(ObsoleteDescriptorBasedAPI::class)
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
            if (rawExpression.isInitializePropertyFromParameter()) {
                // this is a primary constructor property, use corresponding default of value parameter
                defaultsMap.getValue((rawExpression as IrGetValue).symbol)!!
            } else {
                rawExpression
            }
        return expression.deepCopyWithoutPatchingParents().transform(initializerTransformer, null)
    }
}

private fun extractDefaultValuesFromConstructor(irClass: IrClass?): Map<IrValueSymbol, IrExpression?> {
    if (irClass == null) return emptyMap()
    val original = irClass.constructors.singleOrNull { it.isPrimary }
    // default arguments of original constructor
    val defaultsMap: Map<IrValueSymbol, IrExpression?> =
        original?.valueParameters?.associate { it.symbol to it.defaultValue?.expression } ?: emptyMap()
    return defaultsMap + extractDefaultValuesFromConstructor(irClass.getSuperClassNotAny())
}
