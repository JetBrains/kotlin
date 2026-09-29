/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.js.test.blackbox.AbstractJsSecondStageGroupingFacade
import org.jetbrains.kotlin.js.test.blackbox.JsGroupedBatchArtifact
import org.jetbrains.kotlin.js.test.converters.JsFirstStageInvoker
import org.jetbrains.kotlin.test.NonGroupingStageOutput
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.JsIrArtifact
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.artifactsProvider
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider

/**
 * The second stage of the two-stage K/JS test pipeline that links with a previously released compiler, invoked
 * via CLI by means of [CustomJsCompilerSecondStageFacade]. It is meant for KLIB forward-compatibility tests.
 */
class CustomJsCompilerSecondStageGroupingFacade(testServices: TestServices) : AbstractJsSecondStageGroupingFacade(testServices) {
    override fun linkIsolated(output: NonGroupingStageOutput): BinaryArtifacts.Js {
        val services = output.testServices
        val mainModule = JsEnvironmentConfigurator.getMainModule(services)
        val klib = services.artifactsProvider.getArtifact(mainModule, ArtifactKinds.KLib)
        return CustomJsCompilerSecondStageFacade(services).transform(mainModule, klib)
    }

    override fun linkGroupedBatch(batch: GroupedBatch): BinaryArtifacts.Js {
        val services = batch.services

        // The launcher is a product of the first stage, as the KLIBs of the tests are: it is compiled by the current
        // compiler against the libraries of the first stage, and in the ABI the released compiler is able to read.
        val launcherKlibFile = batch.workingDir.resolve("launcher.klib")
        val firstStageRuntimeKlibs = with(services.standardLibrariesPathProvider) {
            listOf(fullJsStdlib().absolutePath, kotlinTestJsKLib().absolutePath)
        }
        JsFirstStageInvoker.compileSourcesToKlib(
            sources = listOf(batch.launcherSource),
            klibOutputFile = launcherKlibFile,
            libraries = firstStageRuntimeKlibs + batch.perTestKlibs,
            languageVersion = customJsCompilerSettings.defaultLanguageVersion,
        )

        val secondStageRuntimeKlibs = listOf(customJsCompilerSettings.stdlib.absolutePath, customJsCompilerSettings.kotlinTest.absolutePath)
        val artifact = CustomJsCompilerSecondStageFacade(services).compileBinary(
            module = batch.launcherModule,
            customArgs = emptyList(),
            mainLibrary = launcherKlibFile.absolutePath,
            regularDependencies = (secondStageRuntimeKlibs + batch.perTestKlibs).toSet(),
            friendDependencies = emptySet(),
        )
        checkTestInfrastructure(artifact is JsIrArtifact) { "Unexpected artifact of a grouped batch: ${artifact::class}" }
        return JsGroupedBatchArtifact(artifact, batch.launcherModule)
    }
}
