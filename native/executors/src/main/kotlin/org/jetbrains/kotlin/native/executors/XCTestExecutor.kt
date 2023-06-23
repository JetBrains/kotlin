/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import org.jetbrains.kotlin.konan.target.*

abstract class AbstractXCTestExecutor(
    private val configurables: AppleConfigurables,
    private val executor: Executor
) : Executor {
    private val hostExecutor = HostExecutor()

    private val target by configurables::target

    private fun targetPlatform(): String {
        val xcodeTarget = when (target) {
            KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64 -> "macosx"
            KonanTarget.IOS_X64, KonanTarget.IOS_SIMULATOR_ARM64 -> "iphonesimulator"
            KonanTarget.IOS_ARM64 -> "iphoneos"
            else -> error("Target $target is not supported buy the executor")
        }

        val request = ExecuteRequest(
            "/usr/bin/xcrun",
            args = mutableListOf("--sdk", xcodeTarget, "--show-sdk-platform-path")
        )
        hostExecutor.execute(request).assertSuccess()

        return request.stdout.toString().trim()
    }

    private val frameworkPath: String
        get() = "${targetPlatform()}/Developer/Library/Frameworks/"

    private val xcTestExecutablePath: String
        get() = "${targetPlatform()}/Developer/Library/Xcode/Agents/xctest"

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        val response = executor.execute(request.copying {
            environment["DYLD_FRAMEWORK_PATH"] = frameworkPath
            val testExecutable = executableAbsolutePath
            executableAbsolutePath = xcTestExecutablePath
            args.add(0, testExecutable)
        })
        return response
    }
}

class XCTestHostExecutor(configurables: AppleConfigurables) : AbstractXCTestExecutor(configurables, HostExecutor())

class XCTestSimulatorExecutor(configurables: AppleConfigurables) :
    AbstractXCTestExecutor(configurables, XcodeSimulatorExecutor(configurables))