/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.facet

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.platform.IdePlatformKind

class KotlinVersionInfoProviderByModuleDependencies : KotlinVersionInfoProvider {
    override fun getCompilerVersion(module: Module) = bundledRuntimeVersion()

    override fun getLibraryVersions(
        module: Module,
        platformKind: IdePlatformKind<*>,
        rootModel: ModuleRootModel?
    ): Collection<String> {
        if (module.isDisposed) {
            return emptyList()
        }

        val versionProvider = platformKind.tooling.getLibraryVersionProvider(module.project)
        return (rootModel ?: ModuleRootManager.getInstance(module))
            .orderEntries
            .asSequence()
            .filterIsInstance<LibraryOrderEntry>()
            .mapNotNull { libraryEntry -> libraryEntry.library?.let { versionProvider(it) } }
            .toList()
    }
}