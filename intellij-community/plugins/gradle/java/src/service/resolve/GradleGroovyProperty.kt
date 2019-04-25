// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.codeInsight.javadoc.JavaDocInfoGenerator
import com.intellij.icons.AllIcons
import com.intellij.ide.presentation.Presentation
import com.intellij.openapi.util.Key
import com.intellij.psi.OriginInfoAwareElement
import com.intellij.psi.PsiElement
import com.intellij.util.lazyPub
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings.GradleProp
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder
import org.jetbrains.plugins.groovy.lang.resolve.api.LazyTypeProperty
import javax.swing.Icon

@Presentation(typeName = "Gradle Property")
class GradleGroovyProperty(
  private val myProperty: GradleProp,
  context: PsiElement
) : LazyTypeProperty(myProperty.name, myProperty.typeFqn, context),
    OriginInfoAwareElement {

  override fun getIcon(flags: Int): Icon? = AllIcons.Nodes.Property

  override fun getOriginInfo(): String? = "via ext"

  private val doc by lazyPub {
    val value = myProperty.value
    val result = StringBuilder()
    result.append("<PRE>")
    JavaDocInfoGenerator.generateType(result, propertyType, context, true)
    result.append(" " + myProperty.name)
    val hasInitializer = !value.isNullOrBlank()
    if (hasInitializer) {
      result.append(" = ")
      val longString = value.toString().length > 100
      if (longString) {
        result.append("<blockquote>")
      }
      result.append(value)
      if (longString) {
        result.append("</blockquote>")
      }
    }
    result.append("</PRE>")
    if (hasInitializer) {
      result.append("<br><b>Initial value has been got during last import</b>")
    }
    result.toString()
  }

  override fun <T : Any?> getUserData(key: Key<T>): T? {
    if (key == NonCodeMembersHolder.DOCUMENTATION) {
      @Suppress("UNCHECKED_CAST")
      return doc as T
    }
    return super.getUserData(key)
  }

  override fun toString(): String = "Gradle Property: $name"
}
