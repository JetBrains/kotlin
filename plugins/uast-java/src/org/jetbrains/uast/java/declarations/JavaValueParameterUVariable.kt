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

import com.intellij.psi.PsiParameter
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class JavaValueParameterUVariable(
        override val psi: PsiParameter,
        override val parent: UElement
) : JavaAbstractUElement(), UVariable, PsiElementBacked, NoModifiers {
    override val name: String
        get() = psi.name.orAnonymous()

    override val nameElement by lz { JavaConverter.convert(psi.nameIdentifier, this) }
    override val type by lz { JavaConverter.convert(psi.type, this) }

    override val initializer: UExpression?
        get() = null

    override val kind: UastVariableKind
        get() = UastVariableKind.VALUE_PARAMETER

    override val annotations by lz { psi.modifierList.getAnnotations(this) }
}