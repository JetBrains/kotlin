/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.analysis.api

import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KtStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.analysis.project.structure.allDirectDependencies
import org.jetbrains.kotlin.tooling.core.withClosureSequence

/**
 * Returns all registered [KtLibraryModule] in this [StandaloneAnalysisAPISession].
 * Note: If a library module is not added as a dependency of another module, make sure to add the module directly as in:
 * ```kotlin
 *  buildKtModuleProvider {
 *      addModule( // <- !! addModule !!
 *          buildKtLibraryModule {
 *              addBinaryRoot(myKlibRootPath)
 *              libraryName = myLibraryName
 *              // ...
 *          }
 *      )
 *  }
 * ```
 */
public fun StandaloneAnalysisAPISession.getAllLibraryModules(): Sequence<KtLibraryModule> {
    val projectStructureProvider = project.getService(ProjectStructureProvider::class.java)
        ?: error("${ProjectStructureProvider::class.java} not found")

    if (projectStructureProvider !is KtStaticProjectStructureProvider) {
        error("Expected implementation of ${KtStaticProjectStructureProvider::class.java} but found ${projectStructureProvider.javaClass}")
    }

    return projectStructureProvider.allKtModules
        .withClosureSequence<KtModule> { module -> module.allDirectDependencies().asIterable() }
        .filterIsInstance<KtLibraryModule>()
}