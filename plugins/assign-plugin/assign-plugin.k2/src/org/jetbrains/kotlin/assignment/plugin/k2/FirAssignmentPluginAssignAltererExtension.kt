/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.k2

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.assignment.plugin.AssignmentPluginNames.ASSIGN_METHOD
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirAssignExpressionAltererExtension
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedNameError
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirAssignmentPluginAssignAltererExtension(
    session: FirSession
) : FirAssignExpressionAltererExtension(session) {

    override fun transformVariableAssignment(variableAssignment: FirVariableAssignment): FirStatement? {
        return runIf(variableAssignment.supportsTransformVariableAssignment()) {
            buildFunctionCall(variableAssignment)
        }
    }

    override fun getOperationName(reference: FirErrorNamedReference): Name? {
        return if (reference.dealsWith(ASSIGN_METHOD) && reference.originallyIs(KtBinaryExpression::class.java)) ASSIGN_METHOD
        else null
    }

    private fun FirVariableAssignment.supportsTransformVariableAssignment(): Boolean {
        return when (val lSymbol = calleeReference?.toResolvedVariableSymbol()) {
            is FirPropertySymbol -> lSymbol.isVal && !lSymbol.isLocal && lSymbol.hasSpecialAnnotation()
            is FirBackingFieldSymbol -> lSymbol.isVal && lSymbol.hasSpecialAnnotation()
            is FirFieldSymbol -> lSymbol.isVal && lSymbol.hasSpecialAnnotation()
            else -> false
        }
    }

    private fun FirVariableSymbol<*>.hasSpecialAnnotation(): Boolean =
        session.annotationMatchingService.isAnnotated(resolvedReturnType.upperBoundIfFlexible().toRegularClassSymbol(session))

    private fun buildFunctionCall(variableAssignment: FirVariableAssignment): FirFunctionCall {
        val leftArgument = variableAssignment.calleeReference!!
        val leftSymbol = leftArgument.toResolvedVariableSymbol()!!
        val leftResolvedType = leftSymbol.resolvedReturnTypeRef
        val rightArgument = variableAssignment.rValue
        return buildFunctionCall {
            source = variableAssignment.source?.fakeElement(KtFakeSourceElementKind.DesugaredCompoundAssignment)
            explicitReceiver = buildPropertyAccessExpression {
                source = leftArgument.source
                typeRef = leftResolvedType
                calleeReference = leftArgument
                (variableAssignment.lValue as? FirQualifiedAccessExpression)?.typeArguments?.let(typeArguments::addAll)
                annotations += variableAssignment.annotations
                explicitReceiver = variableAssignment.explicitReceiver
                dispatchReceiver = variableAssignment.dispatchReceiver
                extensionReceiver = variableAssignment.extensionReceiver
                contextReceiverArguments += variableAssignment.contextReceiverArguments
            }
            argumentList = buildUnaryArgumentList(rightArgument)
            calleeReference = buildSimpleNamedReference {
                source = variableAssignment.source
                name = ASSIGN_METHOD
            }
            origin = FirFunctionCallOrigin.Regular
        }
    }
}

private fun FirErrorNamedReference.dealsWith(name: Name): Boolean {
    return (diagnostic as? ConeUnresolvedNameError)?.name == name
}

@Suppress("UNUSED_PARAMETER")
private inline fun <reified T> FirErrorNamedReference.originallyIs(expressionClass: Class<T>): Boolean {
    return (source as? KtRealPsiSourceElement)?.psi is T
}