// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_PROJECT
import org.jetbrains.plugins.gradle.service.resolve.GradleExtensionsContributor.Companion.getDocumentation
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.GradleExtensionsData
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessProperties

/**
 * @author Vladislav.Soroka
 */
class GradleNonCodeMembersContributor : NonCodeMembersContributor() {
  override fun processDynamicElements(qualifierType: PsiType,
                                      aClass: PsiClass?,
                                      processor: PsiScopeProcessor,
                                      place: PsiElement,
                                      state: ResolveState) {
    if (aClass == null) return
    val containingFile = place.containingFile
    if (!containingFile.isGradleScript() || containingFile?.originalFile?.virtualFile == aClass.containingFile?.originalFile?.virtualFile) return

    if (qualifierType.equalsToText(GRADLE_API_PROJECT)) {
      val propCandidate = place.references.singleOrNull()?.canonicalText ?: return
      val extensionsData: GradleExtensionsData?
      val methodCall = place.children.singleOrNull()
      if (methodCall is GrMethodCallExpression) {
        val projectPath = methodCall.argumentList.expressionArguments.singleOrNull()?.reference?.canonicalText ?: return
        val file = containingFile?.originalFile?.virtualFile ?: return
        val module = ProjectFileIndex.SERVICE.getInstance(place.project).getModuleForFile(file)
        val rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module)
        extensionsData = GradleExtensionsSettings.getInstance(place.project).getExtensionsFor(rootProjectPath, projectPath) ?: return
      }
      else if (methodCall is GrReferenceExpression) {
        if (place.children[0].text == "rootProject") {
          val file = containingFile?.originalFile?.virtualFile ?: return
          val module = ProjectFileIndex.SERVICE.getInstance(place.project).getModuleForFile(file)
          val rootProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module)
          extensionsData = GradleExtensionsSettings.getInstance(place.project).getExtensionsFor(rootProjectPath, ":") ?: return
        }
        else return
      }
      else return

      if (!processor.shouldProcessProperties()) {
        return
      }

      extensionsData.findProperty(propCandidate)?.let {
        val docRef = Ref.create<String>()
        val variable = object : GrLightVariable(place.manager, propCandidate, it.typeFqn, place) {
          override fun getNavigationElement(): PsiElement {
            val navigationElement = super.getNavigationElement()
            navigationElement.putUserData(NonCodeMembersHolder.DOCUMENTATION, docRef.get())
            return navigationElement
          }
        }
        val doc = getDocumentation(it, variable)
        docRef.set(doc)
        place.putUserData(NonCodeMembersHolder.DOCUMENTATION, doc)
        processor.execute(variable, state)
      }
    }
  }
}
