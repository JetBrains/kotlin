/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.konan.config.overrideClangOptions
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.target.AppleConfigurables
import org.jetbrains.kotlin.konan.target.ClangFlags
import org.jetbrains.kotlin.konan.target.RelocationModeFlags
import org.jetbrains.kotlin.konan.target.withOSVersion
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * Uses the clang compiler to compile the bitcode contained in a file.
 */
internal class ClangBitcodeCompiler(
        private val context: NativeBackendPhaseContext,
) : BitcodeCompiler<Path> {

    private val config = context.config
    private val platform = config.platform
    private val optimize = context.shouldOptimize()
    private val debug = config.debug

    private val overrideClangOptions =
            config.configuration.overrideClangOptions

    private fun MutableList<String>.addNonEmpty(elements: List<String>) {
        addAll(elements.filter { it.isNotEmpty() })
    }

    private fun runTool(vararg command: String) =
            Command(*command)
                    .logWith(context::log)
                    .execute()

    private fun targetTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.absoluteTargetToolchain}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun hostLlvmTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.absoluteLlvmHome}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun clang(configurables: ClangFlags, bitcodeFile: Path, objectFile: Path) {
        val targetTriple = if (configurables is AppleConfigurables) {
            platform.targetTriple.withOSVersion(configurables.osVersionMin)
        } else {
            platform.targetTriple
        }
        val flags = overrideClangOptions.takeIf(List<String>::isNotEmpty)
                ?: mutableListOf<String>().apply {
                    addNonEmpty(configurables.clangFlags)
                    addNonEmpty(listOf("-triple", targetTriple.toString()))
                    addNonEmpty(when {
                        optimize -> configurables.clangOptFlags
                        debug -> configurables.clangDebugFlags
                        else -> configurables.clangNooptFlags
                    })
                    addNonEmpty(configurables.currentRelocationMode(context).translateToClangCc1Flag())
                }
        val bitcodePath = bitcodeFile.normalize().absolutePathString()
        val objectPath = objectFile.normalize().absolutePathString()
        if (configurables is AppleConfigurables && config.configuration[BinaryOptions.compileBitcodeWithXcodeLlvm] == true) {
            targetTool("clang++", *flags.toTypedArray(), bitcodePath, "-o", objectPath)
        } else {
            hostLlvmTool("clang++", *flags.toTypedArray(), bitcodePath, "-o", objectPath)
        }
    }

    private fun RelocationModeFlags.Mode.translateToClangCc1Flag() = when (this) {
        RelocationModeFlags.Mode.PIC -> listOf("-mrelocation-model", "pic")
        RelocationModeFlags.Mode.STATIC -> listOf("-mrelocation-model", "static")
        RelocationModeFlags.Mode.DEFAULT -> emptyList()
    }

    /**
     * Compile the bitcode contained in the file [bitcodeContainer] to [outputFilePath].
     */
    override fun makeObjectFile(bitcodeContainer: Path, outputFilePath: Path) =
            when (val configurables = platform.configurables) {
                is ClangFlags -> clang(configurables, bitcodeContainer, outputFilePath)
                else -> error("Unsupported configurables kind: ${configurables::class.simpleName}!")
            }
}