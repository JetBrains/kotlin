// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.ide.presentation.Presentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.plugins.groovy.lang.resolve.api.GroovyPropertyBase

@Presentation(typeName = "Gradle Extension")
class GradleExtensionProperty(name: String, private val type: PsiType?, context: PsiElement) : GroovyPropertyBase(name, context) {

  override fun getPropertyType(): PsiType? = type

  override fun toString(): String = "Gradle Extension: $name"
}
