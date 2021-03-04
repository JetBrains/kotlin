/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.target.*

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

internal class BitcodeCompiler(val context: Context) {

    private val platform = context.config.platform
    private val optimize = context.shouldOptimize()
    private val debug = context.config.debug

    private val overrideClangOptions =
            context.configuration.getList(KonanConfigKeys.OVERRIDE_CLANG_OPTIONS)

    private fun MutableList<String>.addNonEmpty(elements: List<String>) {
        addAll(elements.filter { it.isNotEmpty() })
    }

    private fun runTool(vararg command: String) =
            Command(*command)
                    .logWith(context::log)
                    .execute()

    private fun temporary(name: String, suffix: String): String =
            context.config.tempFiles.create(name, suffix).absolutePath

    private fun targetTool(tool: String, vararg arg: String) {
        val absoluteToolName = if (platform.configurables is AppleConfigurables) {
            "${platform.absoluteTargetToolchain}/usr/bin/$tool"
        } else {
            "${platform.absoluteTargetToolchain}/bin/$tool"
        }
        runTool(absoluteToolName, *arg)
    }

    private fun hostLlvmTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.absoluteLlvmHome}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun clang(configurables: ClangFlags, file: BitcodeFile): ObjectFile {
        val objectFile = temporary("result", ".o")

        val profilingFlags = llvmProfilingFlags().map { listOf("-mllvm", it) }.flatten()

        // TODO: fix with LLVM update.
        val targetTriple = when (context.config.target) {
            // LLVM we use does not have support for arm64_32.
            KonanTarget.WATCHOS_ARM64 -> {
                require(configurables is AppleConfigurables)
                "arm64_32-apple-watchos${configurables.osVersionMin}"
            }
            // Runtime generates bitcode for mythical macos 10.16 because of old Clang.
            // Let's fix it.
            KonanTarget.MACOS_ARM64 -> {
                require(configurables is AppleConfigurables)
                "arm64-apple-macos${configurables.osVersionMin}"
            }
            else -> context.llvm.targetTriple
        }
        val flags = overrideClangOptions.takeIf(List<String>::isNotEmpty)
                ?: mutableListOf<String>().apply {
            addNonEmpty(configurables.clangFlags)
            addNonEmpty(listOf("-triple", targetTriple))
            if (configurables is ZephyrConfigurables) {
                addNonEmpty(configurables.constructClangCC1Args())
            }
            addNonEmpty(when {
                optimize -> configurables.clangOptFlags
                debug -> configurables.clangDebugFlags
                else -> configurables.clangNooptFlags
            })
            addNonEmpty(BitcodeEmbedding.getClangOptions(context.config))
            addNonEmpty(configurables.currentRelocationMode(context).translateToClangCc1Flag())
            addNonEmpty(profilingFlags)
        }
        if (configurables is AppleConfigurables) {
            targetTool("clang++", *flags.toTypedArray(), file, "-o", objectFile)
        } else {
            hostLlvmTool("clang++", *flags.toTypedArray(), file, "-o", objectFile)
        }
        return objectFile
    }

    private fun RelocationModeFlags.Mode.translateToClangCc1Flag() = when (this) {
        RelocationModeFlags.Mode.PIC -> listOf("-mrelocation-model", "pic")
        RelocationModeFlags.Mode.STATIC -> listOf("-mrelocation-model", "static")
        RelocationModeFlags.Mode.DEFAULT -> emptyList()
    }

    private fun llvmProfilingFlags(): List<String> {
        val flags = mutableListOf<String>()
        if (context.shouldProfilePhases()) {
            flags += "-time-passes"
        }
        if (context.inVerbosePhase) {
            flags += "-debug-pass=Structure"
        }
        return flags
    }

    fun makeObjectFiles(bitcodeFile: BitcodeFile): List<ObjectFile> =
            listOf(when (val configurables = platform.configurables) {
                is ClangFlags -> clang(configurables, bitcodeFile)
                else -> error("Unsupported configurables kind: ${configurables::class.simpleName}!")
            })
}