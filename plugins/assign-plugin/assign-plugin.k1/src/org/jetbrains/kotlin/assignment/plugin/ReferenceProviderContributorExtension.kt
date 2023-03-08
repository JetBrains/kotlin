/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin

import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.references.fe10.base.KtFe10KotlinReferenceProviderContributor

class ReferenceProviderContributorExtension : KtFe10KotlinReferenceProviderContributor.Extension {
    override fun getCustomReference(expression: KtSimpleNameExpression): PsiReference? {
        return if (expression is KtOperationReferenceExpression && expression.operationSignTokenType == KtTokens.EQ) {
            KtFe10AssignOperationReference(expression)
        } else null
    }
}