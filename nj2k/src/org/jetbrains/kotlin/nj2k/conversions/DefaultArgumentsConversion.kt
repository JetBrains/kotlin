/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.ConversionContext
import org.jetbrains.kotlin.nj2k.jvmAnnotation
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKFieldAccessExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKUniverseMethodSymbol
import org.jetbrains.kotlin.nj2k.tree.impl.psi
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DefaultArgumentsConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {

    private fun JKMethod.canNotBeMerged(): Boolean =
        modality == Modality.ABSTRACT
                || modality == Modality.OVERRIDE
                || hasExtraModifier(ExtraModifier.NATIVE)
                || hasExtraModifier(ExtraModifier.SYNCHRONIZED)
                || context.converter.converterServices.oldServices.referenceSearcher.hasOverrides(psi()!!)
                || annotationList.annotations.isNotEmpty()


    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        // TODO: Declaration list owner
        if (element !is JKClass) return recurse(element)

        val methods = element.declarationList.filterIsInstance<JKMethod>().sortedBy { it.parameters.size }

        checkMethod@ for (method in methods) {
            val block = method.block as? JKBlock ?: continue
            val singleStatement = block.statements.singleOrNull() ?: continue
            if (method.canNotBeMerged()) continue

            val call = lookupCall(singleStatement) ?: continue
            val callee = call.identifier as? JKUniverseMethodSymbol ?: continue
            val calledMethod = callee.target
            if (calledMethod.parent != method.parent
                || callee.name != method.name.value
                || calledMethod.returnType.type != method.returnType.type
                || call.arguments.arguments.size <= method.parameters.size
            ) {
                continue
            }


            // TODO: Filter by annotations, visibility, modality, extraModifiers like synchronized
            if (calledMethod.visibility != method.visibility) continue@checkMethod
            if (calledMethod.canNotBeMerged()) continue

            for (i in method.parameters.indices) {
                val parameter = method.parameters[i]
                val targetParameter = calledMethod.parameters[i]
                val argument = call.arguments.arguments[i].value
                if (parameter.name.value != targetParameter.name.value) continue@checkMethod
                if (parameter.type.type != targetParameter.type.type) continue@checkMethod
                if (argument !is JKFieldAccessExpression || argument.identifier.target != parameter) continue@checkMethod
            }


            call.arguments.invalidate()
            val defaults = call.arguments.arguments
                .map { it::value.detached() }
                .zip(calledMethod.parameters)
                .drop(method.parameters.size)

            for ((defaultValue, parameter) in defaults) {
                fun remapParameterSymbol(on: JKTreeElement): JKTreeElement {
                    if (on is JKFieldAccessExpression) {
                        val target = on.identifier.target
                        if (target is JKParameter) {
                            val newSymbol =
                                context.symbolProvider.provideUniverseSymbol(calledMethod.parameters[method.parameters.indexOf(target)])
                            return JKFieldAccessExpressionImpl(newSymbol)
                        }
                    }

                    return applyRecursive(on, ::remapParameterSymbol)
                }

                parameter.initializer = remapParameterSymbol(defaultValue) as JKExpression
            }
            element.classBody.declarations -= method
        }

        for (method in element.declarationList) {
            if (method !is JKJavaMethod) continue
            if (method.hasParametersWithDefaultValues() && (method.visibility == Visibility.PUBLIC || method.visibility == Visibility.INTERNAL)) {
                method.annotationList.annotations += jvmAnnotation("JvmOverloads", context.symbolProvider)
            }
        }

        return recurse(element)

    }

    private fun JKMethod.hasParametersWithDefaultValues() =
        parameters.any { it.initializer !is JKStubExpression }

    private fun lookupCall(statement: JKStatement): JKMethodCallExpression? {
        val expression = when (statement) {
            is JKExpressionStatement -> statement.expression
            is JKReturnStatement -> statement.expression
            else -> null
        }
        return when (expression) {
            is JKMethodCallExpression -> expression
            is JKQualifiedExpression -> {
                if (expression.receiver !is JKThisExpression) return null
                expression.selector.safeAs()
            }
            else -> null
        }
    }

}
