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

import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.ULabeledExpression
import org.jetbrains.uast.kotlin.declarations.KotlinUIdentifier

class KotlinULabeledExpression(
        override val psi: KtLabeledExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), ULabeledExpression {
    override val label: String
        get() = psi.getLabelName().orAnonymous("label")

    override val labelIdentifier: UIdentifier?
        get() = psi.getTargetLabel()?.let { KotlinUIdentifier(it, this) }

    override val expression by lz { KotlinConverter.convertOrEmpty(psi.baseExpression, this) }
}