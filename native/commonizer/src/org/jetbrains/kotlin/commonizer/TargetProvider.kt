/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.library.SerializedMetadata
import java.io.File

class TargetProvider(
    val target: CommonizerTarget,
    val modulesProvider: ModulesProvider
)

interface ModulesProvider {
    open class ModuleInfo(
        val name: String,
        val cInteropAttributes: CInteropModuleAttributes?
    )

    class CInteropModuleAttributes(
        val mainPackage: String,
        val exportedForwardDeclarations: Collection<String>
    )

    /**
     * Returns information about all modules that can be loaded by this [ModulesProvider] in the form of [ModuleInfo]s.
     * The module infos are expected to be already loaded. Access should be lightweight
     */
    val moduleInfos: Collection<ModuleInfo>

    /**
     * Loads metadata for the specified module.
     */
    fun loadModuleMetadata(name: String): SerializedMetadata
}
