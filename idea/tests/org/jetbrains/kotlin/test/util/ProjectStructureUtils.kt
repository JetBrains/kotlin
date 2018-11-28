/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        with (library.modifiableModel) {
            classesRoot?.let { addRoot(it, OrderRootType.CLASSES) }
            sourcesRoot?.let { addRoot(it, OrderRootType.SOURCES) }
            commit()
        }
        library
    }
}

val File.jarRoot get() = JarFileSystem.getInstance().getRootByLocal(LocalFileSystem.getInstance().findFileByIoFile(this)!!)!!

fun Module.addDependency(
        library: Library,
        dependencyScope: DependencyScope = DependencyScope.COMPILE,
        exported: Boolean = false
) = ModuleRootModificationUtil.addDependency(this, library, dependencyScope, exported)
