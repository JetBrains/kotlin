/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.*

abstract class ImportInsertHelper {
    /*TODO: implementation is not quite correct*/
    abstract fun isImportedWithDefault(importPath: ImportPath, contextFile: KtFile): Boolean

    abstract fun isImportedWithLowPriorityDefaultImport(importPath: ImportPath, contextFile: KtFile): Boolean

    abstract fun mayImportOnShortenReferences(descriptor: DeclarationDescriptor, contextFile: KtFile): Boolean

    abstract fun getImportSortComparator(contextFile: KtFile): Comparator<ImportPath>

    abstract fun importDescriptor(
        file: KtFile,
        descriptor: DeclarationDescriptor,
        actionRunningMode: ActionRunningMode = ActionRunningMode.RUN_IN_CURRENT_THREAD,
        forceAllUnderImport: Boolean = false
    ): ImportDescriptorResult

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ImportInsertHelper =
            ServiceManager.getService<ImportInsertHelper>(project, ImportInsertHelper::class.java)
    }
}

enum class ImportDescriptorResult {
    FAIL,
    IMPORT_ADDED,
    ALREADY_IMPORTED
}

