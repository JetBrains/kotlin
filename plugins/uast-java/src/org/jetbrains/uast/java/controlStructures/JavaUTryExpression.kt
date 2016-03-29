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

import com.intellij.psi.PsiCatchSection
import com.intellij.psi.PsiTryStatement
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUTryExpression(
        override val psi: PsiTryStatement,
        override val parent: UElement
) : JavaAbstractUElement(), UTryExpression, PsiElementBacked {
    override val tryClause by lz { JavaConverter.convertOrEmpty(psi.tryBlock, this) }
    override val catchClauses by lz { psi.catchSections.map { JavaUCatchClause(it, this) } }
    override val finallyClause by lz { psi.finallyBlock?.let { JavaConverter.convert(it, this) } }
    override val resources by lz {
        val vars = psi.resourceList ?: return@lz null
        val resources = vars.map { JavaConverter.convert(it, this) ?: UDeclarationNotResolved }
        if (resources.isEmpty()) null else resources
    }
}

class JavaUCatchClause(
        override val psi: PsiCatchSection,
        override val parent: UElement
) : JavaAbstractUElement(), UCatchClause, PsiElementBacked {
    override val body by lz { JavaConverter.convertOrEmpty(psi.catchBlock, this) }
    override val parameters by lz { psi.parameter?.let { listOf(JavaConverter.convert(it, this)) } ?: emptyList() }
    override val types by lz { psi.preciseCatchTypes.map { JavaConverter.convert(it, this) } }
}