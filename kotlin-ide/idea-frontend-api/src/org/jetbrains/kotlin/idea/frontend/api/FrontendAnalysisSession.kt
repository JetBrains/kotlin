/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.*

abstract class FrontendAnalysisSession : Invalidatable {
    abstract fun getSmartCastedToTypes(expression: KtExpression): Collection<TypeInfo>?

    abstract fun getImplicitReceiverSmartCasts(expression: KtExpression): Collection<ImplicitReceiverSmartCast>

    abstract fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): TypeInfo?

    abstract fun getKtExpressionType(expression: KtExpression): TypeInfo?

    abstract fun isSubclassOf(klass: KtClassOrObject, superClassId: ClassId): Boolean

    abstract fun getDiagnosticsForElement(element: KtElement): Collection<Diagnostic>

    abstract fun resolveCall(call: KtCallExpression): CallInfo?

    abstract fun resolveCall(call: KtBinaryExpression): CallInfo?
}
