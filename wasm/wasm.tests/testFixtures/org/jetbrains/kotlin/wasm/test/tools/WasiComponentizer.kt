/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.tools

import java.io.File
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

/**
 * Turns the core module of a WASI test into a Wasm component, required for WASI 0.2+ support, e.g. in tests.
 *
 * The steps are the same as in KGP's WasmComponentizeTask (KT-87723)
 *
 * Differences from KGP:
 *  - TODO(KT-89754): A [WitProject] can name the world to embed, because the WIT of the standard library declares more than one 
 *  - TODO(TODO create ticket): There is no WASI preview 1 adapter. If a `wasi_snapshot_preview1` import is left, `component new` fails.
 *
 * TODO(TODO create ticket): share the implementation with `WasmComponentizeTask`.
 */
internal object WasiComponentizer {
    /**
     * A *self-contained* WIT project, meaning the directory must contain all necessary dependencies in the standard deps/ folder.
     */
    class WitProject(val directory: File, val world: String? = null)

    private const val EMBED_DIRECTORY_NAME = "embed"
    private const val COMPONENT_DIRECTORY_NAME = "component"

    private const val STDLIB_IMPORTS_WORLD = "kotlin-stdlib:wasip2/wasip2-imports"
    private const val ENTRY_POINTS_PACKAGE = "kotlin-test:wasi-entry-points@1.0.0"

    private val wasmTools: ExternalTool by lazy {
        ExternalTool(System.getProperty("wasm.tools.path") ?: error("The system property `wasm.tools.path` is not set"))
    }

    /** Test binaries have no `main`, so the componentizer embeds only the imports world of the standard library. */
    private val stdlibImports: WitProject by lazy {
        val directory = System.getProperty("kotlin.wasm.wasi.stdlib.wit.dir")
            ?: error("The system property `kotlin.wasm.wasi.stdlib.wit.dir` is not set")
        WitProject(File(directory), STDLIB_IMPORTS_WORLD)
    }

    private val entryPointProjects = ConcurrentHashMap<String, WitProject>()

    /**
     * A generated WIT project that only exports [entryPoint]. `component new` keeps only the exports of the embedded worlds.
     */
    private fun entryPointProject(entryPoint: String): WitProject = entryPointProjects.computeIfAbsent(entryPoint) {
        val directory = Files.createTempDirectory("wasi-entry-point-$entryPoint-").toFile().apply { deleteOnExit() }
        File(directory, "$entryPoint.wit").apply {
            writeText(
                """
                package $ENTRY_POINTS_PACKAGE;

                world $entryPoint {
                    export $entryPoint: func();
                }
                """.trimIndent()
            )
            deleteOnExit()
        }
        WitProject(directory)
    }

    /**
     * Returns a component of [coreModule] that exports [entryPoint]. This function writes the intermediate modules and the
     * component into directories next to [coreModule].
     */
    fun componentize(coreModule: File, entryPoint: String): File =
        componentize(coreModule, listOf(stdlibImports, entryPointProject(entryPoint)))

    private fun componentize(inputModule: File, witProjects: List<WitProject>): File {
        val outputDirectory = inputModule.absoluteFile.parentFile
        val embedDir = outputDirectory.resolve(EMBED_DIRECTORY_NAME)
        embedDir.deleteRecursively()

        val embedded = witProjects.foldIndexed(inputModule) { index: Int, acc: File, witProject: WitProject ->
            val newOutput = embedDir
                .resolve("$index-${witProject.directory.parentFile.name}-${witProject.directory.name}")
                .resolve(inputModule.name)
            newOutput.parentFile.mkdirs()

            wasmTools.run(
                "component",
                "embed",
                witProject.directory.absolutePath,
                acc.absolutePath,
                *witProject.world?.let { arrayOf("--world", it) }.orEmpty(),
                "-o",
                newOutput.absolutePath,
            )

            newOutput
        }

        val componentDirectory = outputDirectory.resolve(COMPONENT_DIRECTORY_NAME)
        componentDirectory.mkdirs()
        val component = componentDirectory.resolve(inputModule.name)
        wasmTools.run(
            "component",
            "new",
            embedded.absolutePath,
            "-o",
            component.absolutePath,
        )

        return component
    }
}
