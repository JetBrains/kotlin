/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.codegen.kotlinType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.nullIfStubExpression
import org.jetbrains.kotlin.nj2k.qualified
import org.jetbrains.kotlin.nj2k.symbols.*
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.types.*
import org.jetbrains.kotlin.psi.KtObjectDeclaration

import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


class MethodReferenceToLambdaConversion(context: NewJ2kConverterContext) : RecursiveApplicableConversionBase(context) {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKMethodReferenceExpression) return recurse(element)
        val symbol = element.identifier

        val parametersTypesByFunctionalInterface = element
            .functionalType
            .type
            .safeAs<JKClassType>()
            ?.singleFunctionParameterTypes()

        val qualifierExpression = element.qualifier
            .nullIfStubExpression()

        val receiverParameter = if (symbol.safeAs<JKMethodSymbol>()?.isStatic == false && !element.isConstructorCall) {
            val type = when (qualifierExpression) {
                is JKTypeQualifierExpression -> qualifierExpression.type
                is JKClassAccessExpression -> JKClassType(qualifierExpression.identifier)
                else -> null
            }
            if (type != null) {
                JKParameter(
                    JKTypeElement(
                        parametersTypesByFunctionalInterface?.firstOrNull() ?: type
                    ),
                    JKNameIdentifier(RECEIVER_NAME),
                    isVarArgs = false
                )
            } else null
        } else null

        val explicitParameterTypesByFunctionalInterface =
            if (receiverParameter != null) parametersTypesByFunctionalInterface?.drop(1)
            else parametersTypesByFunctionalInterface

        val parameters =
            if (symbol is JKMethodSymbol) {
                (symbol.parameterNames ?: return recurse(element)).zip(
                    explicitParameterTypesByFunctionalInterface ?: symbol.parameterTypes ?: return recurse(element)
                ) { name, type ->
                    JKParameter(
                        JKTypeElement(type),
                        JKNameIdentifier(name),
                        isVarArgs = false
                    )
                }
            } else emptyList()

        val arguments = parameters.map { parameter ->
            val parameterSymbol = symbolProvider.provideUniverseSymbol(parameter)
            JKArgumentImpl(JKFieldAccessExpression(parameterSymbol))
        }
        val callExpression =
            when (symbol) {
                is JKMethodSymbol ->
                    JKCallExpressionImpl(
                        symbol,
                        JKArgumentList(arguments)
                    )
                is JKClassSymbol -> JKNewExpression(symbol, JKArgumentList(), JKTypeArgumentList())
                else -> return recurse(element)
            }
        val qualifier = when {
            receiverParameter != null ->
                JKFieldAccessExpression(symbolProvider.provideUniverseSymbol(receiverParameter))
            element.isConstructorCall -> element.qualifier.safeAs<JKQualifiedExpression>()?.let { it::receiver.detached() }
            else -> element::qualifier.detached().nullIfStubExpression()
        }


        val lambda = JKLambdaExpression(
            JKExpressionStatement(callExpression.qualified(qualifier)),
            listOfNotNull(receiverParameter) + parameters,
            element::functionalType.detached(),
            JKTypeElement(
                when (symbol) {
                    is JKMethodSymbol -> symbol.returnType ?: JKNoType
                    is JKClassSymbol -> JKClassType(symbol)
                    else -> return recurse(element)
                }
            )
        )

        return recurse(lambda)
    }

    private fun JKType.substituteTypeParameters(classType: JKClassType) = applyRecursive { type ->
        if (type is JKTypeParameterType && type.identifier.declaredIn == classType.classReference)
            classType.parameters.getOrNull(type.identifier.index)
        else null
    }


    private fun JKClassType.singleFunctionParameterTypes(): List<JKType>? {
        return when (val reference = classReference) {
            is JKMultiverseClassSymbol -> reference.target.methods
                .firstOrNull { !it.hasModifier(JvmModifier.STATIC) }
                ?.parameterList
                ?.parameters
                ?.map { typeFactory.fromPsiType(it.type).substituteTypeParameters(this) }
            is JKMultiverseKtClassSymbol -> reference.target.body
                ?.functions
                ?.singleOrNull()
                ?.valueParameters
                ?.map { typeFactory.fromKotlinType(it.kotlinType(it.analyze()) ?: return null).substituteTypeParameters(this) }
            is JKUniverseClassSymbol -> reference.target.classBody.declarations.firstIsInstanceOrNull<JKMethod>()
                ?.parameters
                ?.map { it.type.type.substituteTypeParameters(this) }
            else -> null
        }
    }


    private val JKMethodSymbol.isStatic: Boolean
        get() = when (this) {
            is JKMultiverseFunctionSymbol -> target.parent is KtObjectDeclaration
            is JKMultiverseMethodSymbol -> target.hasModifierProperty(PsiModifier.STATIC)
            is JKUniverseMethodSymbol -> target.parent?.parent?.safeAs<JKClass>()?.classKind == JKClass.ClassKind.COMPANION
            is JKUnresolvedMethod -> false
            is KtClassImplicitConstructorSymbol -> false
        }

    private val JKMethodSymbol.parameterNames: List<String>?
        get() {
            return when (this) {
                is JKMultiverseFunctionSymbol -> target.valueParameters.map { it.name ?: return null }
                is JKMultiverseMethodSymbol -> target.parameters.map { it.name ?: return null }
                is JKUniverseMethodSymbol -> target.parameters.map { it.name.value }
                is JKUnresolvedMethod -> null
                is KtClassImplicitConstructorSymbol -> null
            }
        }


    companion object {
        private const val RECEIVER_NAME = "obj" //name taken from old j2k
    }
}