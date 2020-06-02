// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.model.Pointer
import com.intellij.model.presentation.PresentableSymbol
import com.intellij.model.presentation.SymbolPresentation
import icons.GradleIcons
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.plugins.gradle.util.GradleBundle

@Internal
class GradleProjectSymbol(private val myQualifiedName: List<String>) : PresentableSymbol {

  init {
    require(myQualifiedName.isNotEmpty())
  }

  override fun createPointer(): Pointer<GradleProjectSymbol> = Pointer.hardPointer(this)

  val projectName: String get() = myQualifiedName.last()
  val qualifiedName: String get() = myQualifiedName.joinToString(separator = ":")

  private val myPresentation = SymbolPresentation.create(
    GradleIcons.Gradle,
    projectName,
    GradleBundle.message("gradle.project.0", projectName),
    GradleBundle.message("gradle.project.0", qualifiedName)
  )

  override fun getSymbolPresentation(): SymbolPresentation = myPresentation

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GradleProjectSymbol

    if (myQualifiedName != other.myQualifiedName) return false

    return true
  }

  override fun hashCode(): Int {
    return myQualifiedName.hashCode()
  }
}
