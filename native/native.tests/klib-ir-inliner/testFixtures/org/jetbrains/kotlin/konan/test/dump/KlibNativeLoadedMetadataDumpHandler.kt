/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.dump

import org.jetbrains.kotlin.backend.konan.serialization.loadNativeKlibsInTestPipeline
import org.jetbrains.kotlin.cli.common.SessionWithSources
import org.jetbrains.kotlin.cli.common.prepareNativeSessions
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.test.AbstractLoadedMetadataDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.getAllNativeDependenciesPaths
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.nativeEnvironmentConfigurator

class KlibNativeLoadedMetadataDumpHandler(testServices: TestServices) : AbstractLoadedMetadataDumpHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib
) {
    override val targetPlatform: TargetPlatform
        get() = NativePlatforms.unspecifiedNativePlatform
    override val platformAnalyzerServices: PlatformDependentAnalyzerServices
        get() = NativePlatformAnalyzerServices
    override val dependencyKind: DependencyKind
        get() = DependencyKind.Binary

    override fun prepareSessions(
        module: TestModule,
        configuration: CompilerConfiguration,
        environment: VfsBasedProjectEnvironment,
        moduleName: Name,
        libraryList: DependencyListForCliModule,
    ): List<SessionWithSources<KtFile>> {
        val klibs = loadNativeKlibsInTestPipeline(
            configuration = configuration,
            libraryPaths = getAllNativeDependenciesPaths(module, testServices),
            nativeTarget = testServices.nativeEnvironmentConfigurator.getNativeTarget(module),
        )

        return prepareNativeSessions(
            files = emptyList(),
            configuration,
            moduleName,
            klibs.all,
            libraryList,
            extensionRegistrars = emptyList(),
            isCommonSource = { false },
            fileBelongsToModule = { _, _ -> false },
            metadataCompilationMode = false,
        )
    }
}
