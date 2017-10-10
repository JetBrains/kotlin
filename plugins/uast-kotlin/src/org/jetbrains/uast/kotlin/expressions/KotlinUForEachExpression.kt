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

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UForEachExpression
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter
import org.jetbrains.uast.psi.UastPsiParameterNotResolved

class KotlinUForEachExpression(
        override val psi: KtForExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UForEachExpression {
    override val iteratedValue by lz { KotlinConverter.convertOrEmpty(psi.loopRange, this) }
    override val body by lz { KotlinConverter.convertOrEmpty(psi.body, this) }
    
    override val variable by lz {
        val parameter = psi.loopParameter?.let { UastKotlinPsiParameter.create(it, psi, this, 0) } 
                ?: UastPsiParameterNotResolved(psi, KotlinLanguage.INSTANCE)
        KotlinUParameter(parameter, psi, this)
    }

    override val forIdentifier: UIdentifier
        get() = UIdentifier(null, this)
}