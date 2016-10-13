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

import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiType
import org.jetbrains.uast.UClassLiteralExpression
import org.jetbrains.uast.UElement

import org.jetbrains.uast.psi.PsiElementBacked

class JavaUClassLiteralExpression(
        override val psi: PsiClassObjectAccessExpression,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UClassLiteralExpression, PsiElementBacked {
    override val type: PsiType
        get() = psi.type
    
    override val expression by lz { JavaUTypeReferenceExpression(psi.operand, this) }
}