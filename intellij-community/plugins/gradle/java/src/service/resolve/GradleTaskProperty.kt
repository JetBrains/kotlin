// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.ide.presentation.Presentation
import com.intellij.openapi.util.Key
import com.intellij.psi.OriginInfoAwareElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.util.lazyPub
import icons.ExternalSystemIcons
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.GradleTask
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.resolve.api.GroovyPropertyBase
import javax.swing.Icon

@Presentation(typeName = "Gradle Task")
class GradleTaskProperty(
  val task: GradleTask,
  private val myContext: PsiElement
) : GroovyPropertyBase(task.name, myContext),
    OriginInfoAwareElement {

  override fun isValid(): Boolean = super.isValid() && myContext.isValid

  private val type by lazyPub { TypesUtil.createType(task.typeFqn, myContext) }

  override fun getPropertyType(): PsiType? = type

  override fun getIcon(flags: Int): Icon? = ExternalSystemIcons.Task

  override fun getOriginInfo(): String? = "task"

  private val doc by lazyPub { GradleExtensionsContributor.getDocumentation(task, type, myContext) }

  override fun <T : Any?> getUserData(key: Key<T>): T? {
    if (key == NonCodeMembersHolder.DOCUMENTATION) {
      @Suppress("UNCHECKED_CAST")
      return doc as T
    }
    return super.getUserData(key)
  }

  override fun toString(): String = "Gradle Task: $name"
}
