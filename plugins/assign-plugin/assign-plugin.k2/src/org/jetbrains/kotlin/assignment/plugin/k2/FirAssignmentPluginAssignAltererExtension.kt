/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.k2

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.extensions.FirAssignExpressionAltererExtension
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible
import org.jetbrains.kotlin.types.expressions.OperatorConventions.ASSIGN_METHOD
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@OptIn(DirectDeclarationsAccess::class)
class FirAssignmentPluginAssignAltererExtension(
    session: FirSession
) : FirAssignExpressionAltererExtension(session) {

    override fun transformVariableAssignment(variableAssignment: FirVariableAssignment): FirStatement? {
        return runIf(variableAssignment.supportsTransformVariableAssignment()) {
            buildFunctionCall(variableAssignment)
        }
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
        val leftArgument = variableAssignment.calleeReference as FirNamedReference
        val leftSymbol = leftArgument.toResolvedVariableSymbol()!!
        val leftResolvedType = leftSymbol.resolvedReturnTypeRef
        val rightArgument = variableAssignment.rValue
        return buildFunctionCall {
            source = variableAssignment.source?.fakeElement(KtFakeSourceElementKind.AssignmentPluginAltered)
            explicitReceiver = buildPropertyAccessExpression {
                source = leftArgument.source
                coneTypeOrNull = leftResolvedType.coneType
                calleeReference = leftArgument
                (variableAssignment.lValue as? FirQualifiedAccessExpression)?.typeArguments?.let(typeArguments::addAll)
                annotations += variableAssignment.annotations
                explicitReceiver = variableAssignment.explicitReceiver
                dispatchReceiver = variableAssignment.dispatchReceiver
                extensionReceiver = variableAssignment.extensionReceiver
                contextArguments += variableAssignment.contextArguments
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
