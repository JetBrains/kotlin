/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.cli.utilities

import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.util.KonanHomeProvider

fun runLlvmTool(args: Array<String>) {
    val toolName = args[0]
    val toolArguments = args.drop(1)

    val platform = platformManager().hostPlatform
    val llvmHome = platform.configurables.absoluteLlvmHome

    val toolPath = "$llvmHome/bin/$toolName"

    runCommand(toolPath, *toolArguments.toTypedArray())
}

fun runLlvmClangToolWithTarget(args: Array<String>) {
    val toolName = args[0]
    val targetName = args[1]
    val toolArguments = args.drop(2)

    val platformManager = platformManager()
    val platform = platformManager.platform(platformManager.targetByName(targetName))
    val llvmHome = platform.configurables.absoluteLlvmHome

    val toolPath = "$llvmHome/bin/$toolName"

    val compilerArgs: Array<String> = when (toolName) {
        "clang++" -> platform.clang.clangXXArgs
        "clang" -> platform.clang.clangArgs
        else -> error("Unknown tool name: $toolName. Use either `clang` or `clang++`")
    }

    runCommand(toolPath, *compilerArgs, *toolArguments.toTypedArray())
}

private fun platformManager() = PlatformManager(KonanHomeProvider.determineKonanHome())

private fun runCommand(vararg args: String) {
    Command(*args)
            .logWith { println(it()) }
            .execute()
}
