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

package org.jetbrains.kotlin.parcelize.ide.quickfixes

import kotlinx.parcelize.Parcelize
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.diagnostic.ErrorsParcelize
import org.jetbrains.kotlin.parcelize.ide.KotlinParcelizeBundle
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtPsiFactory

class AnnotateWithParcelizeQuickFix(clazz: KtClassOrObject) : AbstractParcelizeQuickFix<KtClassOrObject>(clazz) {
    object Factory : AbstractFactory(
        {
            val targetClass = ErrorsParcelize.CLASS_SHOULD_BE_PARCELIZE.cast(this).a
            AnnotateWithParcelizeQuickFix(targetClass)
        }
    )

    override fun getText() = KotlinParcelizeBundle.message("parcelize.fix.annotate.containing.class.with.parcelize")

    override fun invoke(ktPsiFactory: KtPsiFactory, element: KtClassOrObject) {
        element.addAnnotation(FqName(Parcelize::class.java.name))
    }
}
