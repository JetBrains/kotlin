/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container.assignment.k2

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirAssignExpressionAltererExtension
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.name.Name

class FirValueContainerAssignmentAltererExtension(
    session: FirSession
) : FirAssignExpressionAltererExtension(session) {

    companion object {
        val ASSIGN_METHOD = Name.identifier("assign")
    }

    override fun transformVariableAssignment(variableAssignment: FirVariableAssignment): FirStatement? {
        return if (variableAssignment.supportsTransformVariableAssignment()) {
            buildFunctionCall(variableAssignment)
        } else {
            null
        }
    }

    private fun FirVariableAssignment.supportsTransformVariableAssignment(): Boolean {
        val leftSymbol = lValue.resolvedSymbol as? FirPropertySymbol
        return leftSymbol != null && leftSymbol.isVal && !leftSymbol.isLocal && leftSymbol.hasSpecialAnnotation()
    }

    private fun FirPropertySymbol.hasSpecialAnnotation(): Boolean =
        session.annotationMatchingService.isAnnotated(resolvedReturnType.toRegularClassSymbol(session))

    private fun buildFunctionCall(variableAssignment: FirVariableAssignment): FirFunctionCall {
        val leftArgument = variableAssignment.lValue
        val leftSymbol = leftArgument.resolvedSymbol as FirPropertySymbol
        val leftResolvedType = leftSymbol.resolvedReturnTypeRef
        val rightArgument = variableAssignment.rValue
        return buildFunctionCall {
            source = variableAssignment.source?.fakeElement(KtFakeSourceElementKind.DesugaredCompoundAssignment)
            explicitReceiver = buildPropertyAccessExpression {
                source = leftArgument.source
                typeRef = leftResolvedType
                calleeReference = variableAssignment.calleeReference
                typeArguments += variableAssignment.typeArguments
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
                candidateSymbol = null
            }
            origin = FirFunctionCallOrigin.Regular
        }
    }
}
