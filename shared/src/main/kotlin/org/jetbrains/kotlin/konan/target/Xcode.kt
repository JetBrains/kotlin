/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File

interface Xcode {
    val toolchain: String
    val macosxSdk: String
    val iphoneosSdk: String
    val iphonesimulatorSdk: String
    val version: String
    val appletvosSdk: String
    val appletvsimulatorSdk: String
    val watchosSdk: String
    val watchsimulatorSdk: String
    // Xcode.app/Contents/Developer/usr
    val additionalTools: String
    val simulatorRuntimes: String

    companion object {
        val current: Xcode by lazy {
            CurrentXcode
        }
    }
}

private object CurrentXcode : Xcode {

    override val toolchain by lazy {
        val ldPath = xcrun("-f", "ld") // = $toolchain/usr/bin/ld
        File(ldPath).parentFile.parentFile.parentFile.absolutePath
    }

    override val additionalTools: String by lazy {
        val bitcodeBuildToolPath = xcrun("-f", "bitcode-build-tool")
        File(bitcodeBuildToolPath).parentFile.parentFile.absolutePath
    }

    override val simulatorRuntimes: String by lazy {
        Command("/usr/bin/xcrun", "simctl", "list", "runtimes", "-j").getOutputLines().joinToString(separator = "\n")
    }
    override val macosxSdk by lazy { getSdkPath("macosx") }
    override val iphoneosSdk by lazy { getSdkPath("iphoneos") }
    override val iphonesimulatorSdk by lazy { getSdkPath("iphonesimulator") }
    override val appletvosSdk by lazy { getSdkPath("appletvos") }
    override val appletvsimulatorSdk by lazy { getSdkPath("appletvsimulator") }
    override val watchosSdk: String by lazy { getSdkPath("watchos") }
    override val watchsimulatorSdk: String by lazy { getSdkPath("watchsimulator") }


    override val version by lazy {
        xcrun("xcodebuild", "-version")
                .removePrefix("Xcode ")
    }

    private fun xcrun(vararg args: String): String =
            Command("/usr/bin/xcrun", *args).getOutputLines().first() // TODO: handle execution error

    private fun getSdkPath(sdk: String) = xcrun("--sdk",  sdk, "--show-sdk-path")
}
