// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.codeInsight.javadoc.JavaDocInfoGenerator.generateType
import com.intellij.ide.presentation.Presentation
import com.intellij.openapi.util.Key
import com.intellij.psi.OriginInfoAwareElement
import com.intellij.psi.PsiElement
import com.intellij.util.lazyPub
import icons.ExternalSystemIcons
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.GradleTask
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder.DOCUMENTATION
import org.jetbrains.plugins.groovy.lang.resolve.api.LazyTypeProperty
import javax.swing.Icon

@Presentation(typeName = "Gradle Task")
class GradleTaskProperty(
  val task: GradleTask,
  context: PsiElement
) : LazyTypeProperty(task.name, task.typeFqn, context),
    OriginInfoAwareElement {

  override fun getIcon(flags: Int): Icon? = ExternalSystemIcons.Task

  override fun getOriginInfo(): String? = "task"

  private val doc by lazyPub {
    val result = StringBuilder()
    result.append("<PRE>")
    generateType(result, propertyType, myContext, true)
    result.append(" " + task.name)
    result.append("</PRE>")
    task.description?.let(result::append)
    result.toString()
  }

  override fun <T : Any?> getUserData(key: Key<T>): T? {
    if (key == DOCUMENTATION) {
      @Suppress("UNCHECKED_CAST")
      return doc as T
    }
    return super.getUserData(key)
  }

  override fun toString(): String = "Gradle Task: $name"
}
