// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName

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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LanguageRef

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

data class FileTypeRef(val name: String, val icon: Icon?) {
  companion object {
    @JvmStatic
    fun forFileType(fileType: FileType): FileTypeRef = FileTypeRef(fileType.name, fileType.icon)

    @JvmStatic
    fun forAllFileTypes(): List<FileTypeRef> {
      return FileTypeManager.getInstance().registeredFileTypes
        .sortedWith(FileTypeComparator.INSTANCE)
        .map { forFileType(it) }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FileTypeRef

    if (name != other.name) return false

    return true
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }

}