/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.expressions.v2.irCall
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetField
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetValue
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.isFullValueClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithoutPatchingParents
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

fun IrBuilderWithScope.getProperty(receiver: IrExpression, property: IrProperty, expectedPropertyType: IrType? = null): IrExpression {
    val getter = property.getter
    return if (getter != null)
        irCall(getter.symbol) {
            type = expectedPropertyType ?: getter.returnType
            origin = IrStatementOrigin.GET_PROPERTY
            arguments[0] = receiver
        }
    else
        irGetField(property.backingField!!.symbol) { this.receiver = receiver }
}

/*
  Create a function that creates `get property value expressions` for given corresponded constructor's param
    (constructor_params) -> get_property_value_expression
 */
fun IrBuilderWithScope.createPropertyByParamReplacer(
    irClass: IrClass,
    serialProperties: List<IrSerializableProperty>,
    instance: IrValueParameter
): (IrValueParameterSymbol) -> IrExpression? {
    fun IrSerializableProperty.irGet(): IrExpression {
        val ownerType = instance.symbol.owner.type
        return getProperty(irGetValue(instance.symbol) { type = ownerType }, ir)
    }

    val serialPropertiesMap = serialProperties.associateBy { it.ir }

    val transientPropertiesSet =
        irClass.declarations.asSequence()
            .filterIsInstance<IrProperty>()
            .filter { it.isNonStaticWithField }
            .filter { !serialPropertiesMap.containsKey(it) }
            .toSet()

    return { vpd ->
        val propertyDescriptor = irClass.properties.find { it.name == vpd.owner.name }
        if (propertyDescriptor != null) {
            val value = serialPropertiesMap[propertyDescriptor]
            value?.irGet() ?: run {
                if (propertyDescriptor in transientPropertiesSet)
                    getProperty(irGetValue(instance.symbol), propertyDescriptor)
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
fun createInitializerAdapter(
    irClass: IrClass,
    paramGetReplacer: (IrValueParameterSymbol) -> IrExpression?,
    thisGetReplacer: Pair<IrValueSymbol, () -> IrExpression>? = null
): (IrExpressionBody) -> IrExpression {
    val constructorParameters = irClass.primaryConstructor?.parameters
        .orEmpty()
        .mapTo(hashSetOf()) { it.symbol }

    val initializerTransformer = object : IrElementTransformerVoid() {
        // try to replace `get some value` expression
        override fun visitGetValue(expression: IrGetValue): IrExpression {
            val symbol = expression.symbol
            if (thisGetReplacer != null && thisGetReplacer.first == symbol) {
                // replace `get this value` expression
                return thisGetReplacer.second()
            }

            if (symbol is IrValueParameterSymbol && symbol in constructorParameters) {
                // replace `get parameter value` expression
                paramGetReplacer(symbol)?.let { return it }
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

// For full value classes that assign their fields before `super(...)`, fir2ir moves the initializers of properties declared in the
// primary constructor into the constructor body, so their defaults remain only in the constructor parameters.
fun IrProperty.defaultValueInitializer(): IrExpressionBody? =
    backingField?.initializer ?: (parent as? IrClass)?.takeIf { it.isFullValueClass }
        ?.primaryConstructor?.parameters?.find { it.name == name }?.defaultValue

private fun extractDefaultValuesFromConstructor(irClass: IrClass?): Map<IrValueSymbol, IrExpression?> {
    if (irClass == null) return emptyMap()
    val original = irClass.constructors.singleOrNull { it.isPrimary }
    // default arguments of original constructor
    val defaultsMap: Map<IrValueSymbol, IrExpression?> =
        original?.parameters?.associate { it.symbol to it.defaultValue?.expression } ?: emptyMap()
    return defaultsMap + extractDefaultValuesFromConstructor(irClass.getSuperClassNotAny())
}
