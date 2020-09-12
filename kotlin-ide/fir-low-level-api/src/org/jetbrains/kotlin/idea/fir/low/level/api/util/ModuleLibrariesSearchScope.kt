/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

internal class ModuleLibrariesSearchScope(private val module: Module) : GlobalSearchScope(module.project) {
    override fun contains(file: VirtualFile): Boolean {
        val orderEntry = ModuleRootManager.getInstance(module).fileIndex.getOrderEntryForFile(file)
        return orderEntry is JdkOrderEntry || orderEntry is LibraryOrderEntry
    }

    override fun compare(file1: VirtualFile, file2: VirtualFile): Int {
        val fileIndex = ModuleRootManager.getInstance(module).fileIndex
        return Comparing.compare(fileIndex.getOrderEntryForFile(file2), fileIndex.getOrderEntryForFile(file1))
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean = false

    override fun isSearchInLibraries(): Boolean = true
}