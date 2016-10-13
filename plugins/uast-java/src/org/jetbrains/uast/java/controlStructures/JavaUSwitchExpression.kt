/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.uast.java

import com.intellij.psi.PsiSwitchLabelStatement
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.impl.source.tree.ChildRole
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUSwitchExpression(
        override val psi: PsiSwitchStatement,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), USwitchExpression, PsiElementBacked {
    override val expression by lz { JavaConverter.convertOrEmpty(psi.expression, this) }
    override val body by lz { JavaConverter.convertOrEmpty(psi.body, this) }

    override val switchIdentifier: UIdentifier
        get() = UIdentifier(psi.getChildByRole(ChildRole.SWITCH_KEYWORD), this)
}

class JavaUCaseSwitchClauseExpression(
        override val psi: PsiSwitchLabelStatement,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), USwitchClauseExpression, PsiElementBacked {
    override val caseValues by lz {
        val value = psi.caseValue ?: return@lz null
        listOf(JavaConverter.convertExpression(value, this))
    }
}

class DefaultUSwitchClauseExpression(override val containingElement: UElement?) : USwitchClauseExpression {
    override val caseValues: List<UExpression>?
        get() = null

    override fun asLogString() = "DefaultUSwitchClauseExpression"
    override fun asRenderString() = "else -> "
}