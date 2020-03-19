// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName

import com.intellij.ide.actions.GotoFileAction
import com.intellij.lang.DependentLanguage
import com.intellij.lang.Language
import com.intellij.lang.LanguageUtil
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.PsiElement
import javax.swing.Icon

data class LanguageRef(val id: String, val displayName: String, val icon: Icon?) {
  companion object {
    @JvmStatic
    fun forLanguage(lang: Language): LanguageRef = LanguageRef(lang.id, lang.displayName, lang.associatedFileType?.icon)

    @JvmStatic
    fun forNavigationitem(item: NavigationItem): LanguageRef? = (item as? PsiElement)?.language?.let { forLanguage(it) }

    @JvmStatic
    fun forAllLanguages(): List<LanguageRef> {
      return Language.getRegisteredLanguages()
        .filter { it !== Language.ANY && it !is DependentLanguage }
        .sortedWith(LanguageUtil.LANGUAGE_COMPARATOR)
        .map { forLanguage(it) }
    }
  }
}

data class FileTypeRef(val name: String, val icon: Icon?) {
  companion object {
    @JvmStatic
    fun forFileType(fileType: FileType): FileTypeRef = FileTypeRef(fileType.name, fileType.icon)

    @JvmStatic
    fun forAllFileTypes(): List<FileTypeRef> {
      return FileTypeManager.getInstance().registeredFileTypes
        .sortedWith(GotoFileAction.FileTypeComparator.INSTANCE)
        .map { forFileType(it) }
    }
  }
}