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

import com.intellij.psi.PsiArrayAccessExpression
import org.jetbrains.uast.UArrayAccessExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUArrayAccessExpression(
        override val psi: PsiArrayAccessExpression,
        override val containingElement: UElement?
) : JavaAbstractUExpression(), UArrayAccessExpression, PsiElementBacked {
    override val receiver by lz { JavaConverter.convertExpression(psi.arrayExpression, this) }
    override val indices by lz { singletonListOrEmpty(JavaConverter.convertOrNull(psi.indexExpression, this)) }
}