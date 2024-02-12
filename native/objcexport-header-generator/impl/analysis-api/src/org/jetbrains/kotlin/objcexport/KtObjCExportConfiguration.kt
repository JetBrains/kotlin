/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

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
)
