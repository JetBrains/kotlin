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

import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UIfExpression

class KotlinUIfExpression(
        override val psi: KtIfExpression,
        override val uastParent: UElement?
) : KotlinAbstractUExpression(), UIfExpression, KotlinUElementWithType, KotlinEvaluatableUElement {
    override val condition by lz { KotlinConverter.convertOrEmpty(psi.condition, this) }
    override val thenExpression by lz { KotlinConverter.convertOrNull(psi.then, this) }
    override val elseExpression by lz { KotlinConverter.convertOrNull(psi.`else`, this) }
    override val isTernary = false

    override val ifIdentifier: UIdentifier
        get() = UIdentifier(null, this)

    override val elseIdentifier: UIdentifier?
        get() = null
}