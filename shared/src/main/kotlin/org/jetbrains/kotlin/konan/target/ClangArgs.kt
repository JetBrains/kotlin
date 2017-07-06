/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed -> in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.properties.KonanProperties
import org.jetbrains.kotlin.konan.file.File

class ClangTarget(val target: KonanTarget, val konanProperties: KonanProperties) {

    val sysRoot = konanProperties.absoluteTargetSysRoot!!
    val targetArg = konanProperties.targetArg

    val specificClangArgs: List<String> get() {
        return when (target) {

            KonanTarget.LINUX ->
                listOf("--sysroot=$sysRoot", 
                        "-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=64")
            KonanTarget.RASPBERRYPI ->
                listOf("-target", targetArg!!,
                        "-mfpu=vfp", "-mfloat-abi=hard",
                        "-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=32",
                        "--sysroot=$sysRoot",
                        // TODO: those two are hacks.
                        "-I$sysRoot/usr/include/c++/4.8.3",
                        "-I$sysRoot/usr/include/c++/4.8.3/arm-linux-gnueabihf")

            KonanTarget.MINGW ->
                listOf("-target", targetArg!!, "--sysroot=$sysRoot",
                        "-DUSE_GCC_UNWIND=1", "-DUSE_PE_COFF_SYMBOLS=1", "-DKONAN_WINDOWS=1")

            KonanTarget.MACBOOK ->
                listOf("--sysroot=$sysRoot", "-mmacosx-version-min=10.11")

            KonanTarget.IPHONE ->
                listOf("-stdlib=libc++", "-arch", "arm64", "-isysroot", "$sysRoot", "-miphoneos-version-min=8.0.0")

            KonanTarget.IPHONE_SIM ->
                listOf("-stdlib=libc++", "-isysroot", "$sysRoot", "-miphoneos-version-min=8.0.0")

            KonanTarget.ANDROID_ARM32 ->
                listOf("-target", targetArg!!,
                        "--sysroot=$sysRoot",
                        "-D__ANDROID__", "-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=32", "-DKONAN_ANDROID",
                        // HACKS!
                        "-I$sysRoot/usr/include/c++/4.9.x",
                        "-I$sysRoot/usr/include/c++/4.9.x/arm-linux-androideabi")

            KonanTarget.ANDROID_ARM64 ->
                listOf("-target", targetArg!!,
                        "--sysroot=$sysRoot",
                        "-D__ANDROID__", "-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=64", "-DKONAN_ANDROID",
                        // HACKS!
                        "-I$sysRoot/usr/include/c++/4.9.x",
                        "-I$sysRoot/usr/include/c++/4.9.x/aarch64-linux-android")
        }
    }
}

class ClangHost(val hostProperties: KonanProperties) {

    val targetToolchain get() = hostProperties.absoluteTargetToolchain
    val gccToolchain get() = hostProperties.absoluteGccToolchain
    val sysRoot get() = hostProperties.absoluteTargetSysRoot
    val llvmDir get() = hostProperties.absoluteLlvmHome

    val binDir = when(TargetManager.host) {
        KonanTarget.LINUX -> "$targetToolchain/bin"
        KonanTarget.MINGW -> "$sysRoot/bin"
        KonanTarget.MACBOOK -> "$sysRoot/usr/bin"
        else -> throw TargetSupportException("Unexpected host platform")
    }

    private val extraHostClangArgs = 
        if (TargetManager.host == KonanTarget.LINUX) {
            listOf("--gcc-toolchain=$gccToolchain")
        } else {
            emptyList()
        }

    val commonClangArgs = listOf("-B$binDir") + extraHostClangArgs

    val hostClangPath = listOf("$llvmDir/bin", binDir)

    private val jdkHome = File.jdkHome.absoluteFile.parent

    val hostCompilerArgsForJni = listOf("", TargetManager.jniHostPlatformIncludeDir).map { "-I$jdkHome/include/$it" }
}

