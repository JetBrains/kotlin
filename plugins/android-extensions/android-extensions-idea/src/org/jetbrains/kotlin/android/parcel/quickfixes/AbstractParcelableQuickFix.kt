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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

abstract class AbstractParcelableQuickFix<T: KtElement>(element: T) : KotlinQuickFixAction<T>(element) {
    protected companion object {
        fun <T : KtElement> T.shortenReferences() = ShortenReferences.DEFAULT.process(this)
    }

    override fun getFamilyName() = text

    abstract fun invoke(ktPsiFactory: KtPsiFactory, element: T)

    final override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val clazz = element ?: return
        val ktPsiFactory = KtPsiFactory(project, markGenerated = true)
        invoke(ktPsiFactory, clazz)
    }

    abstract class AbstractFactory(private val f: Diagnostic.() -> IntentionAction?) : KotlinSingleIntentionActionFactory() {
        companion object {
            inline fun <reified T: KtElement> Diagnostic.findElement() = psiElement.getNonStrictParentOfType<T>()
        }

        override fun createAction(diagnostic: Diagnostic) = f(diagnostic)
    }
}