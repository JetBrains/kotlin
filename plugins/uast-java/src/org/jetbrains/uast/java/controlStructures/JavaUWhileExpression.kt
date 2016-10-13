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

import com.intellij.psi.PsiWhileStatement
import com.intellij.psi.impl.source.tree.ChildRole
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UWhileExpression
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUWhileExpression(
        override val psi: PsiWhileStatement,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UWhileExpression, PsiElementBacked {
    override val condition by lz { JavaConverter.convertOrEmpty(psi.condition, this) }
    override val body by lz { JavaConverter.convertOrEmpty(psi.body, this) }

    override val whileIdentifier: UIdentifier
        get() = UIdentifier(psi.getChildByRole(ChildRole.WHILE_KEYWORD), this)
}