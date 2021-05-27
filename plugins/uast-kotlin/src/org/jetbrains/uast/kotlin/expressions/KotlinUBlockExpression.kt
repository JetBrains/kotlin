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

import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.uast.*

open class KotlinUBlockExpression(
    override val sourcePsi: KtBlockExpression,
    givenParent: UElement?
) : KotlinAbstractUBlockExpression(sourcePsi, givenParent) {
    override fun convertParent(): UElement? {
        val directParent = super.convertParent()
        if (directParent is UnknownKotlinExpression && directParent.sourcePsi is KtAnonymousInitializer) {
            val containingUClass = directParent.getContainingUClass() ?: return directParent
            containingUClass.methods.find {
                it is KotlinConstructorUMethod && it.isPrimary || it is KotlinSecondaryConstructorWithInitializersUMethod
            }?.let {
                return it.uastBody
            }
        }
        return directParent
    }
}