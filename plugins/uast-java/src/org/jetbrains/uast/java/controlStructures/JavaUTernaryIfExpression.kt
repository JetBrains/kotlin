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

import com.intellij.psi.PsiConditionalExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UIfExpression
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUTernaryIfExpression(
        override val psi: PsiConditionalExpression,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UIfExpression, PsiElementBacked {
    override val condition by lz { JavaConverter.convertExpression(psi.condition, this) }
    override val thenExpression by lz { JavaConverter.convertOrEmpty(psi.thenExpression, this) }
    override val elseExpression by lz { JavaConverter.convertOrEmpty(psi.elseExpression, this) }

    override val isTernary: Boolean
        get() = true

    override val ifIdentifier: UIdentifier
        get() = UIdentifier(null, this)

    override val elseIdentifier: UIdentifier?
        get() = UIdentifier(null, this)
}