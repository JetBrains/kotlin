// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation

import com.intellij.icons.AllIcons
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.navigation.TargetPopupPresentation
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.impl.EditorTabPresentationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.problems.WolfTheProblemSolver
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.ui.JBColor
import org.jetbrains.annotations.ApiStatus.Experimental
import java.awt.Color
import java.awt.Font
import java.io.File
import javax.swing.Icon

@Experimental
class PsiElementTargetPopupPresentation(private val myElement: PsiElement) : TargetPopupPresentation {

  private val myProject: Project = myElement.project
  private val myVirtualFile: VirtualFile? = myElement.containingFile?.virtualFile
  private val myModule: Module? by lazy { myVirtualFile?.let { ModuleUtil.findModuleForFile(it, myProject) } }
  private val myItemPresentation: ItemPresentation? = (myElement as? NavigationItem)?.presentation

  override fun getIcon(): Icon? = myElement.getIcon(Iconable.ICON_FLAG_VISIBILITY or Iconable.ICON_FLAG_READ_STATUS)

  override fun getPresentableText(): String {
    return myItemPresentation?.presentableText
           ?: (myElement as? PsiNamedElement)?.name
           ?: myElement.text
  }

  private fun getFileBackgroundColor(): Color? {
    val virtualFile = myVirtualFile ?: return null
    return EditorTabPresentationUtil.getFileBackgroundColor(myProject, virtualFile)
  }

  override fun getPresentableAttributes(): TextAttributes? {
    val textAttributes = myItemPresentation?.getColoredAttributes()
    if (textAttributes?.backgroundColor != null) {
      return textAttributes
    }
    val fileColor = getFileBackgroundColor()
    if (fileColor == null) {
      return textAttributes
    }
    val result = textAttributes?.clone() ?: TextAttributes()
    result.backgroundColor = fileColor
    return result
  }

  override fun getLocationText(): String? = myItemPresentation?.getLocationText()

  override fun getLocationAttributes(): TextAttributes? {
    val virtualFile = myVirtualFile ?: return null
    val locationColor = FileStatusManager.getInstance(myProject).getStatus(virtualFile)?.color
    val hasProblem = WolfTheProblemSolver.getInstance(myProject).isProblemFile(virtualFile)
    return when {
      hasProblem -> TextAttributes(locationColor, null, JBColor.red, EffectType.WAVE_UNDERSCORE, Font.PLAIN)
      locationColor != null -> TextAttributes(locationColor, null, null, null, Font.PLAIN)
      else -> null
    }
  }

  override fun getRightText(): String? {
    val virtualFile = myVirtualFile ?: return null
    val fileIndex = ProjectFileIndex.getInstance(myProject)
    if (fileIndex.isInLibrarySource(virtualFile) || fileIndex.isInLibraryClasses(virtualFile)) {
      val jar = JarFileSystem.getInstance().getVirtualFileForJar(virtualFile) ?: return null
      val name = jar.name
      val text = orderEntryText(fileIndex, virtualFile) ?: sdkText(virtualFile) ?: return "($name)"
      return if (text == name) text else "$text ($name)"
    }
    else {
      val module = myModule ?: return null
      if (Registry.`is`("ide.show.folder.name.instead.of.module.name")) {
        val path = ModuleUtilCore.getModuleDirPath(module)
        return if (path.isEmpty()) module.name else File(path).name
      }
      else {
        return module.name
      }
    }
  }

  override fun getRightIcon(): Icon? {
    val virtualFile = myVirtualFile ?: return null
    val fileIndex = ProjectFileIndex.getInstance(myProject)
    return when {
      fileIndex.isInLibrarySource(virtualFile) || fileIndex.isInLibraryClasses(virtualFile) -> AllIcons.Nodes.PpLibFolder
      fileIndex.isInTestSourceContent(virtualFile) -> AllIcons.Nodes.TestSourceFolder
      else -> myModule?.let { ModuleType.get(it) }?.icon
    }
  }
}
