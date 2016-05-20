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

import com.intellij.psi.PsiField
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiVariable
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUVariable(
        override val psi: PsiVariable,
        override val parent: UElement
) : JavaAbstractUElement(), UVariable, PsiElementBacked {
    override val name: String
        get() = psi.name.orAnonymous()

    override val nameElement by lz { JavaDumbUElement(psi.nameIdentifier, this) }
    override val type by lz { JavaConverter.convert(psi.type, this) }

    override val initializer by lz { JavaConverter.convertOrEmpty(psi.initializer, this) }

    override val kind = when (psi) {
        is PsiField -> UastVariableKind.MEMBER
        is PsiLocalVariable -> UastVariableKind.LOCAL_VARIABLE
        else -> UastVariableKind.LOCAL_VARIABLE
    }

    override val visibility: UastVisibility
        get() = psi.getVisibility()

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)
    override val annotations by lz { psi.modifierList.getAnnotations(this) }
}