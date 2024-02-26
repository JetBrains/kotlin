/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule

data class KtObjCExportConfiguration(
    /**
     * Also used as top level prefix for declarations if present
     */
    val frameworkName: String? = null,

    /**
     * Should kdoc written by the user be available in the generated headers and stubs
     */
    val exportKDoc: Boolean = true,

    /**
     * Used to handle Swift outer and inner classes naming edge cases, see:
     * - [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamingHelper.canBeSwiftOuter]
     * - [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamingHelper.canBeSwiftInner]
     */
    val objcGenerics: Boolean = true,

    /**
     * Flag to enable/disable the emission of 'base declarations'
     * (see [org.jetbrains.kotlin.objcexport.objCBaseDeclarations]).
     */
    val generateBaseDeclarationStubs: Boolean = true,

    /**
     * The name of modules that are to be exported in this session.
     * An exported library shall be read, and its entire API surface shall be translated and presented in the
     * objc header at the end.
     *
     * Libraries/Modules that are not listed in this [exportedModuleNames] will only export types that are used in either source code
     * or other exported libraries public surface
     * (see [KtLibraryModule.libraryName], [KtSourceModule.moduleName])
     */
    val exportedModuleNames: Set<String> = emptySet(),
)

