/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKFieldAccessExpressionImpl
import org.jetbrains.kotlin.nj2k.tree.impl.JKUniverseMethodSymbol
import org.jetbrains.kotlin.nj2k.tree.impl.psi
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class DefaultArgumentsConversion(private val context: NewJ2kConverterContext) : RecursiveApplicableConversionBase() {
    private fun JKMethod.canBeGetterOrSetter() =
        name.value.asGetterName() != null
                || name.value.asSetterName() != null

    private fun JKMethod.canNotBeMerged(): Boolean =
        modality == Modality.ABSTRACT
                || hasOtherModifier(OtherModifier.OVERRIDE)
                || hasOtherModifier(OtherModifier.NATIVE)
                || hasOtherModifier(OtherModifier.SYNCHRONIZED)
                || context.converter.converterServices.oldServices.referenceSearcher.hasOverrides(psi()!!)
                || annotationList.annotations.isNotEmpty()
                || canBeGetterOrSetter()


    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKClassBody) return recurse(element)

        val methods = element.declarations.filterIsInstance<JKMethod>().sortedBy { it.parameters.size }

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


            if (calledMethod.visibility != method.visibility) continue@checkMethod
            if (calledMethod.canNotBeMerged()) continue

            for (i in method.parameters.indices) {
                val parameter = method.parameters[i]
                val targetParameter = calledMethod.parameters[i]
                val argument = call.arguments.arguments[i].value
                if (parameter.name.value != targetParameter.name.value) continue@checkMethod
                if (parameter.type.type != targetParameter.type.type) continue@checkMethod
                if (argument !is JKFieldAccessExpression || argument.identifier.target != parameter) continue@checkMethod
                if (parameter.initializer !is JKStubExpression
                    && targetParameter.initializer !is JKStubExpression
                    && !areTheSameExpressions(targetParameter.initializer, parameter.initializer)
                ) continue@checkMethod
            }

            for (i in method.parameters.indices) {
                val parameter = method.parameters[i]
                val targetParameter = calledMethod.parameters[i]
                if (parameter.initializer !is JKStubExpression
                    && targetParameter.initializer is JKStubExpression
                ) {
                    targetParameter.initializer = parameter.initializer.copyTreeAndDetach()
                }
            }



            for (index in (method.parameters.lastIndex + 1)..calledMethod.parameters.lastIndex) {
                val calleeExpression = call.arguments.arguments[index].value
                val defaultArgument = calledMethod.parameters[index].initializer.takeIf { it !is JKStubExpression } ?: continue
                if (!areTheSameExpressions(calleeExpression, defaultArgument)) continue@checkMethod
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
            element.declarations -= method
            calledMethod.appendNonCodeElementsFrom(method)
        }

        for (method in element.declarations) {
            if (method !is JKJavaMethod) continue
            if (method.hasParametersWithDefaultValues()
                && (method.visibility == Visibility.PUBLIC || method.visibility == Visibility.INTERNAL)
            ) {
                method.annotationList.annotations += jvmAnnotation("JvmOverloads", context.symbolProvider)
            }
        }

        return recurse(element)

    }

    private fun areTheSameExpressions(first: JKElement, second: JKElement): Boolean {
        if (first::class != second::class) return false
        if (first is JKNameIdentifier && second is JKNameIdentifier) return first.value == second.value
        if (first is JKLiteralExpression && second is JKLiteralExpression) return first.literal == second.literal
        if (first is JKFieldAccessExpression && second is JKFieldAccessExpression && first.identifier != second.identifier) return false
        if (first is JKMethodCallExpression && second is JKMethodCallExpression && first.identifier != second.identifier) return false
        return if (first is JKBranchElement && second is JKBranchElement) {
            first.children.zip(second.children) { childOfFirst, childOfSecond ->
                when {
                    childOfFirst is JKBranchElement && childOfSecond is JKBranchElement -> {
                        areTheSameExpressions(
                            childOfFirst,
                            childOfSecond
                        )
                    }
                    childOfFirst is List<*> && childOfSecond is List<*> -> {
                        childOfFirst.zip(childOfSecond) { child1, child2 ->
                            areTheSameExpressions(
                                child1 as JKElement,
                                child2 as JKElement
                            )
                        }.reduce(Boolean::and)
                    }
                    else -> false
                }
            }.reduce(Boolean::and)
        } else false
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
