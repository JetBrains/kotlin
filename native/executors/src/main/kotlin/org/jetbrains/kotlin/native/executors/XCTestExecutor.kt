/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import org.jetbrains.kotlin.konan.target.*
import java.io.ByteArrayOutputStream
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

/**
 * An abstract class representing a local execution of XCTest tests.
 *
 * This class is a composable executor that relies on the underlying [Executor].
 * It is able to run xctest on the local host passing arguments as a key-value
 * to the Info.plist file
 *
 * @property configurables The Apple target to be run on.
 * @property executor The underlying executor to execute target-specific operations.
 */
abstract class AbstractXCTestExecutor(
    private val configurables: AppleConfigurables,
    private val executor: Executor,
) : Executor {
    private val hostExecutor = HostExecutor()

    private val target by configurables::target

    init {
        require(HostManager.host.family.isAppleFamily) {
            "$this executor isn't available for $configurables"
        }
    }

    private fun targetPlatform(): String {
        val xcodeTarget = when (target) {
            KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64 -> "macosx"
            KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64 -> "iphonesimulator"
            KonanTarget.IOS_ARM64 -> "iphoneos"
            else -> error("Target $target is not supported buy the executor")
        }

        val stdout = ByteArrayOutputStream()
        val request = ExecuteRequest(
            "/usr/bin/xcrun",
            args = mutableListOf("--sdk", xcodeTarget, "--show-sdk-platform-path"),
            stdout = stdout
        )
        hostExecutor.execute(request).assertSuccess()

        return stdout.toString("UTF-8").trim()
    }

    private val frameworkPath: String by lazy {
        "${targetPlatform()}/Developer/Library/Frameworks/"
    }

    private val xcTestExecutablePath: String by lazy {
        "${targetPlatform()}/Developer/Library/Xcode/Agents/xctest"
    }

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        val bundle = XCTestBundle.Standalone(Paths.get(request.executableAbsolutePath), request.args)
        val path = bundle.prepareToRun(request.workingDirectory?.toPath() ?: Paths.get("."))

        val response = executor.execute(
            request.copy(
                executableAbsolutePath = xcTestExecutablePath,
                args = mutableListOf(path.absolutePathString()),
                environment = request.environment.toMap(mutableMapOf())
                    .also {
                        it["DYLD_FRAMEWORK_PATH"] = frameworkPath
                        it["KotlinNativeTestArgs"] = bundle.args.joinToString(" ")
                    }
            )
        )

        bundle.cleanup()

        return response
    }
}

/**
 * XCTest executor that runs tests on a host machine.
 * It extends [AbstractXCTestExecutor] and uses [HostExecutor] to handle host-specific execution.
 *
 * @param configurables a test execution target configurables.
 */
class XCTestHostExecutor(configurables: AppleConfigurables) : AbstractXCTestExecutor(configurables, HostExecutor()) {
    init {
        require(configurables.target in supportedTargets()) {
            "$this executor is unable to run ${configurables.target}"
        }
    }

    private fun supportedTargets(): List<KonanTarget> = listOf(HostManager.host)
}

/**
 * XCTest executor that runs tests on simulators on a host machine.
 * It extends [AbstractXCTestExecutor] and uses [XcodeSimulatorExecutor] to handle simulator-specific operations.
 *
 * @param configurables a test execution target configurables.
 */
class XCTestSimulatorExecutor(configurables: AppleConfigurables) :
    AbstractXCTestExecutor(configurables, XcodeSimulatorExecutor(configurables)) {
    init {
        require(configurables.target in supportedTargets()) {
            "$this executor is unable to run ${configurables.target}"
        }
    }

    private fun supportedTargets(): List<KonanTarget> = when (HostManager.host) {
        KonanTarget.MACOS_X64 -> listOf(KonanTarget.IOS_X64)
        KonanTarget.MACOS_ARM64 -> listOf(KonanTarget.IOS_SIMULATOR_ARM64)
        else -> emptyList()
    }
}