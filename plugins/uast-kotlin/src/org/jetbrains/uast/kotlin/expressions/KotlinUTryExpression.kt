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

import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.declarations.KotlinUIdentifier

class KotlinUTryExpression(
        override val sourcePsi: KtTryExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UTryExpression, KotlinUElementWithType {
    override val tryClause by lz { KotlinConverter.convertOrEmpty(sourcePsi.tryBlock, this) }
    override val catchClauses by lz { sourcePsi.catchClauses.map { KotlinUCatchClause(it, this) } }
    override val finallyClause by lz {
        sourcePsi.finallyBlock?.finalExpression?.let {
            KotlinConverter.convertExpression(
                it,
                this,
                DEFAULT_EXPRESSION_TYPES_LIST
            )
        }
    }

    override val resourceVariables: List<UVariable>
        get() = emptyList()

    override val hasResources: Boolean
        get() = false

    override val tryIdentifier: UIdentifier
        get() = KotlinUIdentifier(null, this)

    override val finallyIdentifier: UIdentifier?
        get() = null
}