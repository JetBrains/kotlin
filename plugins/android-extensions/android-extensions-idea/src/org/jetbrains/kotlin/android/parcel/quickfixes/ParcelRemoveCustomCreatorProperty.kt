/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.parcel.quickfixes

import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory

class ParcelRemoveCustomCreatorProperty(property: KtProperty) : AbstractParcelableQuickFix<KtProperty>(property) {
    object Factory : AbstractFactory(f@ {
        // KtProperty or its name identifier
        psiElement as? KtProperty ?: (psiElement.parent as? KtProperty) ?: return@f null
        findElement<KtProperty>()?.let(::ParcelRemoveCustomCreatorProperty)
    })

    override fun getText() = "Remove custom ''CREATOR'' property"

    override fun invoke(ktPsiFactory: KtPsiFactory, element: KtProperty) {
        element.delete()
    }
}