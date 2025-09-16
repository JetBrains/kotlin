/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.artifacts

import org.jetbrains.kotlin.utils.MainFunctionCandidate
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.pickMainFunctionFromCandidates
import org.jetbrains.kotlin.utils.putToMultiMap

typealias CachedTestFunctionsWithTheirPackage = Map<String, List<String>>

/**
 * Generates artifacts for per-file compilation mode from Kotlin modules.
 *
 *  Most of the time, a single [Artifact] corresponds to a single Kotlin [File]. However, in case there are multiple files with the same
 *  name and package in one module, they are merged into a single [Artifact].
 *
 *  We also generate an additional _proxy_ [Artifact] for each module, which re-exports the declarations from all the artifacts generated
 *  from files in this module.
 */
interface PerFileGenerator<Module, File, Artifact, TestEnvironment> {
    val mainModuleName: String

    val Module.isMain: Boolean
    val Module.fileList: Iterable<File>

    /**
     * The name of the generated artifact. There can be multiple artifacts with the same [artifactName], for example, if there are multiple
     * files with the same name and package in a single module. In that case, they are merged into a single artifact.
     */
    val Artifact.artifactName: String

    /**
     * Whether importing this artifact has any side effects. In Kotlin terms, this basically means whether the corresponding file contains
     * `@EagerInitialization`-annotated top-level properties.
     */
    val Artifact.hasEffect: Boolean

    /**
     * Whether this artifact has any exported declarations (`@JsExport`).
     */
    val Artifact.hasExport: Boolean

    val Artifact.packageFqn: String

    /**
     * If this artifact contains the `main` function, returns the `main` function's tag
     * (i.e., a unique string that corresponds to the declaration).
     */
    val Artifact.mainFunction: String?

    fun Artifact.takeTestEnvironmentOwnership(): TestEnvironment?
    val TestEnvironment.testFunctionTag: String
    val TestEnvironment.suiteFunctionTag: String

    fun List<Artifact>.merge(): Artifact
    fun File.generateArtifact(module: Module): Artifact?

    /**
     * Generates a special _proxy_ artifact for this module, which re-exports the declarations from all the artifacts generated from
     * files in this module.
     */
    fun Module.generateArtifact(
        mainFunctionTag: String?,
        suiteFunctionTag: String?,
        testFunctions: CachedTestFunctionsWithTheirPackage,
        moduleNameForEffects: String?
    ): Artifact

    /**
     * The Kotlin modules to produce artifacts for.
     *
     * Important: the main module (the one for which [isMain] returns true) should always go last.
     */
    fun generatePerFileArtifacts(modules: List<Module>): List<Artifact> {
        var someModuleHasEffect = false

        val nameToModulePerFile = buildMap {
            for (module in modules) {
                var hasModuleLevelEffect = false
                var hasFileWithExportedDeclaration = false
                var suiteFunctionTag: String? = null
                val testFunctions = mutableMapOf<String, MutableList<String>>()

                val artifacts = module.fileList.mapNotNull {
                    val generatedArtifact = it.generateArtifact(module) ?: return@mapNotNull null

                    if (generatedArtifact.hasExport) {
                        hasFileWithExportedDeclaration = true
                    }

                    if (generatedArtifact.hasEffect) {
                        hasModuleLevelEffect = true
                    }

                    generatedArtifact.takeTestEnvironmentOwnership()?.let { testEnvironment ->
                        testFunctions.putToMultiMap(generatedArtifact.packageFqn, testEnvironment.testFunctionTag)
                        suiteFunctionTag = testEnvironment.suiteFunctionTag
                    }

                    putToMultiMap(generatedArtifact.artifactName, generatedArtifact)

                    generatedArtifact
                }

                if (hasModuleLevelEffect) {
                    someModuleHasEffect = true
                }

                val mainFunctionTag = runIf(module.isMain) {
                    pickMainFunctionFromCandidates(artifacts) {
                        MainFunctionCandidate(
                            it.packageFqn,
                            it.mainFunction
                        )
                    }?.mainFunction
                }

                // Here we make use of the fact that the main module is always the last one in the list, so the last condition is
                // well-formed.
                if (mainFunctionTag != null || hasFileWithExportedDeclaration || hasModuleLevelEffect || suiteFunctionTag != null || (module.isMain && someModuleHasEffect)) {
                    val proxyArtifact = module.generateArtifact(
                        mainFunctionTag,
                        suiteFunctionTag,
                        testFunctions,
                        mainModuleName.takeIf { !module.isMain && hasModuleLevelEffect }
                    ) ?: continue
                    putToMultiMap(proxyArtifact.artifactName, proxyArtifact)
                }
            }
        }

        return nameToModulePerFile.values.map { it.merge() }
    }
}
