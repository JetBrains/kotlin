/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import org.jetbrains.kotlin.konan.target.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import kotlin.io.path.exists

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

        val stdout = ByteArrayOutputStream()
        val request = ExecuteRequest(
            "/usr/bin/xcrun",
            args = mutableListOf("--sdk", xcodeTarget, "--show-sdk-platform-path"),
            stdout = stdout
        )
        hostExecutor.execute(request).assertSuccess()

        return stdout.toString("UTF-8").trim()
    }

    private val frameworkPath: String
        get() = "${targetPlatform()}/Developer/Library/Frameworks/"

    private val xcTestExecutablePath: String
        get() = "${targetPlatform()}/Developer/Library/Xcode/Agents/xctest"

    override fun execute(request: ExecuteRequest): ExecuteResponse {
        val bundlePath = if (request.args.isNotEmpty()) {
            // Copy the bundle to a temp dir
            val dir = Files.createTempDirectory("tmp-xctest-runner")
            dir.toFile().deleteOnExit()
            val newBundlePath = File(request.executableAbsolutePath).run {
                val newPath = dir.resolve(name)
                copyRecursively(newPath.toFile())
                newPath
            }
            check(newBundlePath.exists())

            // Passing arguments to the XCTest-runner using Info.plist file.
            val infoPlist = newBundlePath.toFile()
                .walk()
                .firstOrNull { it.name == "Info.plist" }
                ?.absolutePath
            checkNotNull(infoPlist) { "Info.plist of xctest-bundle wasn't found. Check the bundle contents and location "}

            val writeArgsRequest = ExecuteRequest(
                executableAbsolutePath = "/usr/libexec/PlistBuddy",
                args = mutableListOf("-c", "Add :KotlinNativeTestArgs string ${request.args.joinToString(" ")}", infoPlist)
            )
            val writeResponse = hostExecutor.execute(writeArgsRequest)
            writeResponse.assertSuccess()

            newBundlePath.toString()
        } else {
            request.executableAbsolutePath
        }

        val response = executor.execute(request.copying {
            environment["DYLD_FRAMEWORK_PATH"] = frameworkPath
            executableAbsolutePath = xcTestExecutablePath
            args.clear()
            args.add(bundlePath)
        })

        if (request.executableAbsolutePath != bundlePath) {
            // Remove the copied bundle after the run
            File(bundlePath).deleteRecursively()
        }
        return response
    }
}

class XCTestHostExecutor(configurables: AppleConfigurables) : AbstractXCTestExecutor(configurables, HostExecutor())

class XCTestSimulatorExecutor(configurables: AppleConfigurables) :
    AbstractXCTestExecutor(configurables, XcodeSimulatorExecutor(configurables))