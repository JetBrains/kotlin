/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult

/**
 * Simpler version of [KonanConfig] which is enough for src -> klib compilation.
 */
class LightKonanConfig(
    override val project: Project,
    override val configuration: CompilerConfiguration,
    distributionKlibPath: String,
) : AbstractKonanConfig {
    override val target: KonanTarget = HostManager().targetManager(configuration.get(KonanConfigKeys.TARGET)).target

    private val resolve: KonanLibrariesResolveSupport = KonanLibrariesResolveSupport(
            configuration, target, distributionKlibPath, resolveManifestDependenciesLenient = true
    )

    override val includedLibraries: List<KonanLibrary>
        get() = resolve.includedLibraries

    override val resolvedLibraries: KotlinLibraryResolveResult
        get() = resolve.resolvedLibraries

    override val exportedLibraries: List<KonanLibrary>
        get() = resolve.exportedLibraries

    override val moduleId: String
        get() = configuration.get(KonanConfigKeys.MODULE_NAME)!!

    override val manifestProperties: Properties? = configuration.get(KonanConfigKeys.MANIFEST_FILE)?.let {
        File(it).loadProperties()
    }

    override val fullExportedNamePrefix: String
        get() = configuration.get(KonanConfigKeys.FULL_EXPORTED_NAME_PREFIX)!!
}