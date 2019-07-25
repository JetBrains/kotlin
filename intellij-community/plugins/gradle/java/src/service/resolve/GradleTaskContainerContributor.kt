// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_TASK_CONTAINER
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames.GROOVY_LANG_CLOSURE
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.getName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessMethods
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessProperties

class GradleTaskContainerContributor : NonCodeMembersContributor() {

  override fun getParentClassName(): String? = GRADLE_API_TASK_CONTAINER

  override fun processDynamicElements(qualifierType: PsiType,
                                      aClass: PsiClass?,
                                      processor: PsiScopeProcessor,
                                      place: PsiElement,
                                      state: ResolveState) {
    if (qualifierType !is GradleProjectAwareType) return

    val processProperties = processor.shouldProcessProperties()
    val processMethods = processor.shouldProcessMethods()
    if (!processProperties && !processMethods) {
      return
    }

    val file = place.containingFile ?: return
    val data = GradleExtensionsContributor.getExtensionsFor(file) ?: return

    val name = processor.getName(state)
    val tasks = if (name == null) data.tasksMap.values else listOf(data.tasksMap[name] ?: return)
    if (tasks.isEmpty()) return

    val manager = file.manager
    val closureType = createType(GROOVY_LANG_CLOSURE, file)

    for (task in tasks) {
      val taskType = createType(task.typeFqn, file)
      if (processProperties) {
        val property = GradleTaskProperty(task, file)
        if (!processor.execute(property, state)) return
      }
      if (processMethods) {
        val method = GrLightMethodBuilder(manager, task.name).apply {
          returnType = taskType
          addParameter("configuration", closureType)
        }
        if (!processor.execute(method, state)) return
      }
    }
  }
}
