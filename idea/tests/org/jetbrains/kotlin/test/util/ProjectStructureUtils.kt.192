/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestCase
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import java.io.File

fun PlatformTestCase.projectLibrary(
    libraryName: String = "TestLibrary",
    classesRoot: VirtualFile? = null,
    sourcesRoot: VirtualFile? = null,
    kind: PersistentLibraryKind<*>? = null
): Library {
    return runWriteAction {
        val modifiableModel = ProjectLibraryTable.getInstance(project).modifiableModel
        val library = try {
            modifiableModel.createLibrary(libraryName, kind)
        } finally {
            modifiableModel.commit()
        }
        with(library.modifiableModel) {
            classesRoot?.let { addRoot(it, OrderRootType.CLASSES) }
            sourcesRoot?.let { addRoot(it, OrderRootType.SOURCES) }
            commit()
        }
        library
    }
}

val File.jarRoot: VirtualFile
    get() {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(this) ?: error("Cannot find file $this")
        return JarFileSystem.getInstance().getRootByLocal(virtualFile) ?: error("Can't find root by file $virtualFile")
    }

fun Module.addDependency(
    library: Library,
    dependencyScope: DependencyScope = DependencyScope.COMPILE,
    exported: Boolean = false
) = ModuleRootModificationUtil.addDependency(this, library, dependencyScope, exported)
