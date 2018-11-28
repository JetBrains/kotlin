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

package org.jetbrains.kotlin.idea.spring.lineMarking

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.spring.SpringBundle
import com.intellij.spring.SpringManager
import com.intellij.spring.contexts.model.diagram.gutter.ModelDependenciesLineMarkerProviderJava
import com.intellij.spring.model.jam.stereotype.SpringConfiguration
import com.intellij.spring.model.utils.SpringCommonUtils
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Function
import icons.SpringCoreIcons
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.spring.diagram.OpenKotlinSpringModelDependenciesAction
import org.jetbrains.kotlin.idea.spring.diagram.OpenKotlinSpringModelDependenciesModuleDiagramAction
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch

class KotlinSpringModelDependenciesLineMarkerProvider : ModelDependenciesLineMarkerProviderJava() {
    override fun getId() = javaClass.name

    override fun getName() = "Model Dependencies Graph (Kotlin)"

    override fun isRelevantPsiElement(psiElement: PsiElement): Boolean {
        val lightClass = psiElement.getParentOfTypeAndBranch<KtClass> { nameIdentifier }?.toLightClass() ?: return false
        return SpringConfiguration.PSI_CLASS_PATTERN.accepts(lightClass) && SpringCommonUtils.isConfigurationOrMeta(lightClass)
    }

    override fun doAnnotate(element: PsiElement): LineMarkerInfo<*>? {
        val klass = element.getParentOfTypeAndBranch<KtClass> { nameIdentifier } ?: return null
        val lightClass = klass.toLightClass() ?: return null
        if (SpringManager.getInstance(element.project).getLocalSpringModel(lightClass) == null) return null
        return createLineMarkerInfo(klass)
    }

    private fun createLineMarkerInfo(klass: KtClass): LineMarkerInfo<PsiElement>? {
        val nameIdentifier = klass.nameIdentifier ?: return null
        return LineMarkerInfo(
                nameIdentifier,
                nameIdentifier.textRange,
                SpringCoreIcons.SpringModelsDependencyGraph,
                Pass.UPDATE_ALL,
                Function { SpringBundle.message("local.model.dependencies.diagram.title") },
                createNavigationHandler(klass),
                GutterIconRenderer.Alignment.RIGHT
        )
    }

    private fun createNavigationHandler(klass: KtClass): GutterIconNavigationHandler<PsiElement> {
        return GutterIconNavigationHandler { event, element ->
            val group = DefaultActionGroup().apply {
                add(OpenKotlinSpringModelDependenciesModuleDiagramAction())
                add(OpenKotlinSpringModelDependenciesAction(klass))
            }
            val context = SimpleDataContext.getProjectContext(null)
            JBPopupFactory.getInstance()
                    .createActionGroupPopup(null, group, context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true)
                    .show(RelativePoint(event))
        }
    }
}