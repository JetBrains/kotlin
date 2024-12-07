/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.analysis.api

import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KotlinStaticProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.allDirectDependencies
import org.jetbrains.kotlin.tooling.core.withClosureSequence

/**
 * Returns all registered [KaLibraryModule] in this [StandaloneAnalysisAPISession].
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
public fun StandaloneAnalysisAPISession.getAllLibraryModules(): Sequence<KaLibraryModule> {
    val projectStructureProvider = KotlinProjectStructureProvider.getInstance(project)
    if (projectStructureProvider !is KotlinStaticProjectStructureProvider) {
        error("Expected implementation of ${KotlinStaticProjectStructureProvider::class.java} but found ${projectStructureProvider.javaClass}")
    }

    return projectStructureProvider.allModules
        .withClosureSequence<KaModule> { module -> module.allDirectDependencies().asIterable() }
        .filterIsInstance<KaLibraryModule>()
}