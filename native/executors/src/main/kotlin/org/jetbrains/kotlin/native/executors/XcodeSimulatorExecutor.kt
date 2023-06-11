/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.logging.Logger

private fun defaultDeviceId(target: KonanTarget) = when (target.family) {
    Family.TVOS -> "com.apple.CoreSimulator.SimDeviceType.Apple-TV-4K-4K"
    Family.IOS -> "com.apple.CoreSimulator.SimDeviceType.iPhone-14"
    Family.WATCHOS -> "com.apple.CoreSimulator.SimDeviceType.Apple-Watch-Series-6-40mm"
    else -> error("Unexpected simulation target: $target")
}

private fun Executor.run(executableAbsolutePath: String, vararg args: String) = ByteArrayOutputStream().let {
    this.execute(ExecuteRequest(executableAbsolutePath).apply {
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
        var deviceId: String = defaultDeviceId(configurables.target),
) : Executor {
    private val hostExecutor: Executor = HostExecutor()

    private val logger = Logger.getLogger(this::class.java.name)

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

    private fun simctl(vararg args: String): String {
        val out = hostExecutor.run("/usr/bin/xcrun", *arrayOf("simctl", *args))
        return out.toString("UTF-8").trim()
    }

    private var deviceChecked: SimulatorDeviceDescriptor? = null

    private fun ensureSimulatorExists() {
        // Already ensured that simulator for `deviceId` exists.
        if (deviceId == deviceChecked?.deviceTypeIdentifier) {
            logger.info("Device already exists: ${deviceChecked?.deviceTypeIdentifier} with name ${deviceChecked?.name}")
            return
        }
        logger.info("Device was not cheked before. Find the device with id: $deviceId")
        val simulatorRuntime = getSimulatorRuntime()
        logger.info("Runtime used for the $deviceId is $simulatorRuntime")

        val device = getDeviceFor(simulatorRuntime)
        logger.info("Found device: $device")
        // If successfully found, remember that.
        deviceChecked = device
    }

    private fun getDeviceFor(simulatorRuntime: SimulatorRuntimeDescriptor): SimulatorDeviceDescriptor {
        val runtimeIdentifier = simulatorRuntime.identifier
        val deviceOrNull = {
            getSimulatorDevices(simctl("list", "devices", "--json"))[runtimeIdentifier]
                    ?.find { it.deviceTypeIdentifier == deviceId && it.isAvailable == true }
        }
        // If the device already exists, nothing to do.
        deviceOrNull()?.let { return it }

        // Create the device
        logger.info("Current runtime doesn't have $deviceId available")
        val deviceType = simulatorRuntime.supportedDeviceTypes.find { it.identifier == deviceId }
        checkNotNull(deviceType) {
            """
            Default device $deviceId is not available for the runtime ${simulatorRuntime.name}
            Supported devices: ${simulatorRuntime.supportedDeviceTypes.map { it.identifier }.joinToString(", ")}
            """.trimIndent()
        }
        val out = simctl("create", deviceType.name, deviceType.identifier, runtimeIdentifier)
        logger.info("Create device $deviceId: simctl output is: $out")

        // Return freshly created device if it fits
        deviceOrNull()?.let { return it }

        // Looks like device is unavailable. This may happen if
        // `~/Library/Developer/CoreSimulator/RuntimeMap.plist` has empty preferred runtimes
        logger.info("Runtimes are available but devices can't use it. Download runtimes again with Xcode")
        downloadRuntimeFor(simulatorOsName(target.family))

        return checkNotNull(deviceOrNull()) { "Unable to get or create simulator device $deviceId for $target with runtime ${simulatorRuntime.name}" }
    }

    private fun getSimulatorRuntime(): SimulatorRuntimeDescriptor {
        val simulatorRuntimeOrNull = {
            getSimulatorRuntimesFor(
                    json = simctl("list", "runtimes", "--json"),
                    family = target.family,
                    osMinVersion = configurables.osVersionMin
            ).firstOrNull {
                it.supportedDeviceTypes.any { deviceType -> deviceType.identifier == deviceId }
            }
        }
        // If the simulator runtime already exists, nothing to do.
        simulatorRuntimeOrNull()?.let { return it }

        // Download this platform if not available
        logger.info("Runtime for the $deviceId is not available. Downloading runtimes for $target with Xcode")
        downloadRuntimeFor(simulatorOsName(target.family))

        return checkNotNull(simulatorRuntimeOrNull()) { "Runtime is not available for the selected $deviceId. Check Xcode installation" }
    }

    private fun downloadRuntimeFor(osName: String) {
        val version = Xcode.findCurrent().version
        check(version.major >= 14) {
            "Was unable to get the required runtimes running on Xcode $version. Check the Xcode installation"
        }
        if (version.minor >= 1) {
            // Option -downloadPlatform NAME available only since 14.1
            hostExecutor.run("/usr/bin/xcrun", "xcodebuild", "-downloadPlatform", osName)
        } else {
            // Have to download all platforms :(
            hostExecutor.run("/usr/bin/xcrun", "xcodebuild", "-downloadAllPlatforms")
        }
    }

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        ensureSimulatorExists()
        val executable = request.executableAbsolutePath
        val env = request.environment.mapKeys {
            "SIMCTL_CHILD_" + it.key
        }
        val workingDirectory = request.workingDirectory ?: File(request.executableAbsolutePath).parentFile
        val name = deviceChecked?.name ?: error("No device available for $deviceId")
        // Starting Xcode 11 `simctl spawn` requires explicit `--standalone` flag.
        return hostExecutor.execute(request.copying {
            this.executableAbsolutePath = "/usr/bin/xcrun"
            this.workingDirectory = workingDirectory
            this.args.addAll(0, listOf("simctl", "spawn", "--standalone", *archSpecification, name, executable))
            this.environment.clear()
            this.environment.putAll(env)
        })
    }
}