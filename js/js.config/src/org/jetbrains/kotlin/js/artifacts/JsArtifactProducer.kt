/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.artifacts

import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.js.config.JsGenerationGranularity

/**
 * Something that, given a list of Kotlin [Module]s, produces a bunch of JavaScript/TypeScript [Artifact]s, the number of which
 * depends on [JsGenerationGranularity].
 */
interface JsArtifactProducer<Module, File, Artifact, TestEnvironment> {

    /**
     * @property exportModule The module with only exported declarations, or `null` if there are no exported declarations in the module.
     */
    data class ArtifactModules<Artifact>(val mainModule: Artifact, val exportModule: Artifact? = null)

    /**
     * Transforms a Kotlin [Module] into a single JavaScript/TypeScript [Artifact].
     */
    fun singleModuleToArtifact(module: Module, mainModule: Module): Artifact

    /**
     * In the case of the per-file granularity, produces a bunch of JavaScript/TypeScript [Artifact]s from a single module.
     */
    fun makePerFileGenerator(mainModule: Module): PerFileGenerator<Module, File, ArtifactModules<Artifact>, TestEnvironment>

    /**
     * Produces a bunch of JavaScript/TypeScript [Artifact]s from Kotlin [modules]. The contents of the returned list depend on
     * the [granularity].
     *
     * Note: the returned value is the same for the whole-program and per-module granularities. If the granularity is whole-program,
     * the returned artifacts are supposed to be merged later.
     *
     * Important: the main module should always go last.
     */
    fun generateArtifacts(modules: List<Module>, granularity: JsGenerationGranularity): List<Artifact> = when (granularity) {
        JsGenerationGranularity.WHOLE_PROGRAM, JsGenerationGranularity.PER_MODULE -> {
            val mainModule = modules.last()
            modules.map {
                singleModuleToArtifact(it, mainModule)
            }
        }
        JsGenerationGranularity.PER_FILE -> {
            val perFileGenerator = makePerFileGenerator(mainModule = modules.last())
            buildList {
                for ((mainModule, exportModule) in perFileGenerator.generatePerFileArtifacts(modules)) {
                    add(mainModule)
                    addIfNotNull(exportModule)
                }
            }
        }
    }
}
