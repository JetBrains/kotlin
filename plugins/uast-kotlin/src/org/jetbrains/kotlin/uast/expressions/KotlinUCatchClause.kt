/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.uast

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.uast.UCatchClause
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UType
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUCatchClause(
        override val psi: KtCatchClause,
        override val parent: UElement
) : KotlinAbstractUElement(), UCatchClause, PsiElementBacked {
    override val body by lz { KotlinConverter.convertOrEmpty(psi.catchBody, this) }
    override val parameters by lz { psi.catchParameter?.let { listOf(KotlinConverter.convert(it, this)) } ?: emptyList<UVariable>() }
    override val types by lz {
        val catchParameter = psi.catchParameter ?: return@lz emptyList<UType>()
        val bindingContext = psi.analyze(BodyResolveMode.PARTIAL)
        val parameter = bindingContext[BindingContext.VALUE_PARAMETER, catchParameter] ?: return@lz emptyList<UType>()
        listOf(KotlinConverter.convert(parameter.type, psi.project, this))
    }
}