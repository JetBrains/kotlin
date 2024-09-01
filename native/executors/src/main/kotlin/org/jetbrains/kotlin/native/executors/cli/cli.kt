/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:JvmName("ExecutorsCLI")

package org.jetbrains.kotlin.native.executors.cli

import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.native.executors.ExecuteRequest
import org.jetbrains.kotlin.native.executors.HostExecutor
import org.jetbrains.kotlin.native.executors.RosettaExecutor
import org.jetbrains.kotlin.native.executors.XcodeSimulatorExecutor
import kotlin.system.exitProcess
import kotlin.time.Duration

private data class Args(
    val dist: String,
    val dataDir: String?,
    val target: String?,
    val timeout: String?,
    val executable: String,
    val args: List<String>,
)

private fun usage() {
    println(
        """
        |USAGE: <executors-cli>
        |            --dist=<path to distribution directory>
        |            [--data-dir=<path to data directory>]
        |            [--target=<target for which to run executable>]
        |            [--timeout=<max time allowed to run executable>]
        |            --
        |            <path to executable>
        |            [<additional argument for the executable>]*
    """.trimMargin()
    )
}

private fun Array<String>.parse(): Args {
    var dist: String? = null
    var dataDir: String? = null
    var target: String? = null
    var timeout: String? = null
    var executable: String? = null
    var args: List<String>? = null
    for ((index, arg) in withIndex()) {
        when {
            arg == "--" -> {
                executable = this[index + 1]
                args = this.slice(index + 2 until this.size)
                break
            }
            arg.startsWith("--dist") -> dist = arg.replace("--dist=", "")
            arg.startsWith("--data-dir") -> dataDir = arg.replace("--data-dir=", "")
            arg.startsWith("--target") -> target = arg.replace("--target=", "")
            arg.startsWith("--timeout") -> timeout = arg.replace("--timeout=", "")
            else -> {
                error("Unknown argument `$arg`.")
            }
        }
    }
    return Args(
        dist ?: error("--dist=<...> must be specified"),
        dataDir,
        target,
        timeout,
        executable ?: error("executable must be specified after --"),
        args!!, // if executable is not null, args cannot be null
    )
}

private fun run(args: Args): Nothing {
    val distribution = buildDistribution(args.dist, konanDataDir = args.dataDir)
    val platformManager = PlatformManager(distribution)
    val hostTarget = HostManager.host
    val target = args.target?.let { platformManager.targetByName(it) } ?: hostTarget
    val configurables = platformManager.platform(target).configurables
    val executor = when {
        target == hostTarget -> HostExecutor()
        configurables is AppleConfigurables && configurables.targetTriple.isSimulator -> XcodeSimulatorExecutor(configurables)
        configurables is AppleConfigurables && RosettaExecutor.availableFor(configurables) -> RosettaExecutor(configurables)
        else -> error("Cannot run for target $target")
    }
    val response = executor.execute(ExecuteRequest(args.executable).apply {
        this.args.addAll(args.args)
        args.timeout?.let { this.timeout = Duration.parse(it) }
    })
    exitProcess(response.exitCode ?: -1)
}


fun main(args: Array<String>): Unit = run(try {
    args.parse()
} catch (e: Exception) {
    e.message?.let { println(it) }
    usage()
    exitProcess(1)
})