/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.executors

import java.io.File
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

/**
 * Represents an XCTest bundle.
 */
internal sealed interface XCTestBundle {
    /**
     * Arguments to be passed to the bundle during the execution
     */
    val args: List<String>

    /**
     * Prepares the bundle to execution.
     *
     * @param workingDirectory The working directory where the bundle should be stored and executed
     * @return The resolved path to the bundle
     */
    fun prepareToRun(workingDirectory: Path): Path

    /**
     * Cleanup temp files and directories used during the preparation process.
     */
    fun cleanup() {}

    /**
     * Represents a Xcode project wrapped bundle.
     *
     * This is a bundle stored in `PlugIns` directory of the built application.
     * This bundle makes all necessary preparations like building project and ad-hoc codesign.
     */
    class ProjectWrapped(private var originalBundle: Path, override val args: List<String>) : XCTestBundle {
        private var xcodeProject: XcodeProject? = null

        override fun prepareToRun(workingDirectory: Path): Path {
            xcodeProject = XcodeProject(workingDirectory).apply {
                fetch()
                build()
                replaceTestBundleWith(originalBundle)
                testBundle.toFile().writeTestArguments(args)
                codesign()
            }
            return xcodeProject?.products ?: error("Incorrect")
        }

        @OptIn(ExperimentalPathApi::class)
        override fun cleanup() {
            xcodeProject?.path?.deleteRecursively()
        }

        /**
         * Used to pass [args] to the XCTest-runner using Info.plist file.
         * See also `NativeTestRunner` for details of argument processing.
         */
        private fun File.writeTestArguments(args: List<String>) {
            val infoPlist = walk()
                .firstOrNull { it.name == "Info.plist" }
                ?.absolutePath
                ?: error("Info.plist of xctest-bundle wasn't found. Check the bundle contents and location")

            // TODO: Consider also check for other incorrect symbols and escaping them or use CDATA section.
            check(args.none { it.contains(" ") }) {
                "Provided arguments contain spaces that not supported as arguments: ${args.joinToString()}"
            }

            val writeArgsRequest = ExecuteRequest(
                executableAbsolutePath = "/usr/libexec/PlistBuddy",
                args = mutableListOf("-c", "Add :KotlinNativeTestArgs string ${args.joinToString(" ")}", infoPlist)
            )
            HostExecutor().execute(writeArgsRequest).assertSuccess()
        }
    }

    /**
     * Standalone test bundle ready for execution by `xctest` utility on the host or simulator
     */
    class Standalone(private val originalBundle: Path, override val args: List<String>) : XCTestBundle {
        override fun prepareToRun(workingDirectory: Path): Path = originalBundle
    }
}