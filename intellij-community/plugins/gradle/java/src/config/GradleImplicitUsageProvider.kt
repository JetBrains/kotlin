// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.config

import com.intellij.codeInsight.daemon.ImplicitUsageProvider
import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import com.intellij.psi.util.PropertyUtilBase.*
import com.intellij.usages.impl.rules.UsageType
import com.intellij.util.Processor
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_TASKS_ACTION
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField

/**
 * @author Vladislav.Soroka
 */
class GradleImplicitUsageProvider : ImplicitUsageProvider {
  companion object {
    val taskAnnotations = listOf("org.gradle.api.tasks.Input",
                                 "org.gradle.api.tasks.InputFile",
                                 "org.gradle.api.tasks.InputFiles",
                                 "org.gradle.api.tasks.InputDirectory",
                                 "org.gradle.api.tasks.OutputDirectory",
                                 "org.gradle.api.tasks.OutputDirectories",
                                 "org.gradle.api.tasks.OutputFile",
                                 "org.gradle.api.tasks.LocalState",
                                 "org.gradle.api.tasks.Destroys",
                                 "org.gradle.api.tasks.Classpath",
                                 "org.gradle.api.tasks.Console")
  }

  override fun isImplicitUsage(element: PsiElement): Boolean {
    var modifierList: PsiModifierList? = null
    if (element is GrField) {
      modifierList = element.modifierList
    }
    else if (element is PsiMethod) {
      if (element.modifierList.hasAnnotation(GRADLE_API_TASKS_ACTION)) return true
      if (isSimplePropertyGetter(element)) {
        modifierList = element.modifierList
      }
      else if (isSimplePropertySetter(element)) {
        val getter = getPropertyName(element)?.let { findPropertyGetter(element.containingClass, it, false, false) }
        modifierList = getter?.modifierList
      }
    }
    if (modifierList?.let { taskAnnotations.find(modifierList::hasAnnotation) } != null) {
      return true
    }
    return hasUsageOfType(element, null)
  }

  override fun isImplicitRead(element: PsiElement): Boolean {
    return hasUsageOfType(element, UsageType.READ)
  }

  override fun isImplicitWrite(element: PsiElement): Boolean {
    return hasUsageOfType(element, UsageType.WRITE)
  }

  private fun hasUsageOfType(element: PsiElement, usage: UsageType?): Boolean {
    if (element !is PsiMember) return false

    val found = Ref.create(false)
    GradleUseScopeEnlarger.search(element, Processor {

      if (!it.isReferenceTo(element))
        return@Processor true

      if (usage == null) {
        found.set(true)
        return@Processor false
      }

      val readWriteAccessDetector = ReadWriteAccessDetector.findDetector(element) ?: return@Processor true
      when (readWriteAccessDetector.getReferenceAccess(it.element, it)) {
        ReadWriteAccessDetector.Access.ReadWrite -> {
          found.set(true)
          return@Processor false
        }
        ReadWriteAccessDetector.Access.Read -> {
          if (usage == UsageType.READ) {
            found.set(true)
            return@Processor false
          }
        }
        ReadWriteAccessDetector.Access.Write -> {
          if (usage == UsageType.WRITE) {
            found.set(true)
            return@Processor false
          }
        }
      }
      true
    })
    return found.get()
  }
}
