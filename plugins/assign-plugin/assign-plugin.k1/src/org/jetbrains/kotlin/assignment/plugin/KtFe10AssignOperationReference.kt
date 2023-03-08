/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.references.fe10.KtFe10SimpleNameReference


class KtFe10AssignOperationReference(expression: KtOperationReferenceExpression) : KtFe10SimpleNameReference(expression) {
    override val resolvesByNames: Collection<Name>
        get() {
            val element = element as? KtOperationReferenceExpression ?: error("unexpected type of 'element'")
            val tokenType = element.operationSignTokenType
            return if (tokenType != null) {
                listOf(Name.identifier("assign"))
            } else {
                listOf(element.getReferencedNameAsName())
            }
        }
}