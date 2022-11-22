/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.executors

import org.jetbrains.kotlin.konan.target.*
import java.io.ByteArrayOutputStream
import java.io.File

private fun defaultDeviceName(target: KonanTarget) = when (target.family) {
    Family.TVOS -> "Apple TV 4K"
    Family.IOS -> "iPhone 11"
    Family.WATCHOS -> "Apple Watch Series 6 " + (if (Xcode.findCurrent().version.startsWith("14")) "(40mm)" else "- 40mm")
    else -> error("Unexpected simulation target: $target")
}

private fun Executor.run(executableAbsolutePath: String, vararg args: String) = ByteArrayOutputStream().let {
    this.execute(executeRequest(executableAbsolutePath).apply {
        this.args.addAll(args)
        stdout = it
        workingDirectory = File("").absoluteFile
    }).assertSuccess()
    it
}

/**
 * [Executor] that runs the process in an Xcode simulator.
 *
 * @param configurables [Configurables] for simulated target
 * @property deviceName which simulator to use (optional). When not provided the default simulator for [configurables.target] is used
 */
class XcodeSimulatorExecutor(
        private val configurables: AppleConfigurables,
        var deviceName: String = defaultDeviceName(configurables.target),
) : Executor {
    private val hostExecutor: Executor = HostExecutor()

    private val target by configurables::target

    init {
        require(configurables.targetTriple.isSimulator) {
            "$target is not a simulator."
        }
        val hostArch = HostManager.host.architecture
        val targetArch = target.architecture
        val compatibleArchs = when (hostArch) {
            Architecture.X64 -> listOf(Architecture.X64, Architecture.X86)
            Architecture.ARM64 -> listOf(Architecture.ARM64, Architecture.ARM32)
            else -> throw IllegalStateException("$hostArch is not a supported host architecture for the simulator")
        }
        require(targetArch in compatibleArchs) {
            "Can't run simulator for $targetArch architecture on $hostArch host architecture"
        }
    }

    private val archSpecification = when (target.architecture) {
        Architecture.X86 -> listOf("-a", "i386")
        Architecture.X64 -> listOf() // x86-64 is used by default on Intel Macs.
        Architecture.ARM64 -> listOf() // arm64 is used by default on Apple Silicon.
        else -> error("${target.architecture} can't be used in simulator.")
    }.toTypedArray()

    private var _deviceNameChecked: String? = null
    private fun ensureSimulatorExists() {
        // Already ensured that simulator for `deviceName` exists.
        if (deviceName == _deviceNameChecked)
            return
        // Find out if the default device is available
        val out = hostExecutor.run("/usr/bin/xcrun", "simctl", "list", "devices", "available")
        val deviceNameExists = out.toString("UTF-8").trim().contains(deviceName)
        // Create if it's not available
        if (!deviceNameExists) {
            hostExecutor.run("/usr/bin/xcrun", "simctl", "create", deviceName, deviceName)
        }
        // If successfully created, remember that.
        _deviceNameChecked = deviceName
    }

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        ensureSimulatorExists()
        val executable = request.executableAbsolutePath
        val env = request.environment.mapKeys {
            "SIMCTL_CHILD_" + it.key
        }
        val workingDirectory = request.workingDirectory ?: File(request.executableAbsolutePath).parentFile
        // Starting Xcode 11 `simctl spawn` requires explicit `--standalone` flag.
        return hostExecutor.execute(request.copying {
            this.executableAbsolutePath = "/usr/bin/xcrun"
            this.workingDirectory = workingDirectory
            this.args.addAll(0, listOf("simctl", "spawn", "--standalone", *archSpecification, deviceName, executable))
            this.environment.clear()
            this.environment.putAll(env)
        })
    }
}