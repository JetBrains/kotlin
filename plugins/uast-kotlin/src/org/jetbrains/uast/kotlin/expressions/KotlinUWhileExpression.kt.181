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

import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UWhileExpression
import org.jetbrains.uast.kotlin.declarations.KotlinUIdentifier

class KotlinUWhileExpression(
        override val psi: KtWhileExpression,
        givenParent: UElement?
) : KotlinAbstractUExpression(givenParent), UWhileExpression {
    override val condition by lz { KotlinConverter.convertOrEmpty(psi.condition, this) }
    override val body by lz { KotlinConverter.convertOrEmpty(psi.body, this) }

    override val whileIdentifier: UIdentifier
        get() = KotlinUIdentifier(null, this)
}