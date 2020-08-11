/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeProvider
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*

/**
 * The entry point into all frontend-related work. Has the following contracts:
 * - Should not be accessed from event dispatch thread
 * - Should not be accessed outside read action
 * - Should not be leaked outside read action it was created in
 * - To be sure that session is not leaked it is forbidden to store it in a variable, consider working with it only in [analyze] context
 * - All entities retrieved from analysis facade should not be leaked outside the read action KtAnalysisSession was created in
 *
 * To pass a symbol from one read action to another use [KtSymbolPointer] which can be created from a symbol by [KtSymbol.createPointer]
 *
 * To create analysis session consider using [analyze]
 */
abstract class KtAnalysisSession(override val token: ValidityToken) : ValidityTokenOwner {
    abstract val symbolProvider: KtSymbolProvider
    abstract val scopeProvider: KtScopeProvider

    abstract fun getSmartCastedToTypes(expression: KtExpression): Collection<KtType>?

    abstract fun getImplicitReceiverSmartCasts(expression: KtExpression): Collection<ImplicitReceiverSmartCast>

    abstract fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType

    abstract fun getKtExpressionType(expression: KtExpression): KtType

    abstract fun isSubclassOf(klass: KtClassOrObject, superClassId: ClassId): Boolean

    abstract fun getDiagnosticsForElement(element: KtElement): Collection<Diagnostic>

    abstract fun resolveCall(call: KtCallExpression): CallInfo?
    abstract fun resolveCall(call: KtBinaryExpression): CallInfo?

    abstract fun createContextDependentCopy(): KtAnalysisSession
}
