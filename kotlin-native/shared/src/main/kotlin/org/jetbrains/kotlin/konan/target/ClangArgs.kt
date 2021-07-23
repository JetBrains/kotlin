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

import org.jetbrains.kotlin.konan.file.File

internal object Android {
    const val API = "21"
    private val architectureMap = mapOf(
            KonanTarget.ANDROID_X86 to "x86",
            KonanTarget.ANDROID_X64 to "x86_64",
            KonanTarget.ANDROID_ARM32 to "arm",
            KonanTarget.ANDROID_ARM64 to "arm64"
    )

    fun architectureDirForTarget(target: KonanTarget) =
            "android-${API}/arch-${architectureMap.getValue(target)}"
}

class ClangArgs(private val configurables: Configurables) {

    private val absoluteTargetToolchain = configurables.absoluteTargetToolchain
    private val absoluteTargetSysRoot = configurables.absoluteTargetSysRoot
    private val absoluteLlvmHome = configurables.absoluteLlvmHome
    private val target = configurables.target
    private val targetTriple = configurables.targetTriple

    private val clangArgsSpecificForKonanSources
        get() = configurables.runtimeDefinitions.map { "-D$it" }

    private val binDir = when (HostManager.host) {
        KonanTarget.LINUX_X64 -> "$absoluteTargetToolchain/bin"
        KonanTarget.MINGW_X64 -> "$absoluteTargetToolchain/bin"
        KonanTarget.MACOS_X64,
        KonanTarget.MACOS_ARM64 -> "$absoluteTargetToolchain/usr/bin"
        else -> throw TargetSupportException("Unexpected host platform")
    }
    // TODO: Use buildList
    private val commonClangArgs: List<String> = mutableListOf<List<String>>().apply {
        add(listOf("-B$binDir", "-fno-stack-protector"))
        if (configurables is GccConfigurables) {
            add(listOf("--gcc-toolchain=${configurables.absoluteGccToolchain}"))
        }
        if (configurables is AppleConfigurables) {
            val osVersionMin = when (target) {
                // Here we workaround Clang 8 limitation: macOS major version should be 10.
                // So we compile runtime with version 10.16 and then override version in BitcodeCompiler.
                // TODO: Fix with LLVM Update.
                KonanTarget.MACOS_ARM64 -> "10.16"
                else -> configurables.osVersionMin
            }
            val targetArg = targetTriple.copy(
                    architecture = when (targetTriple.architecture) {
                        // TODO: LLVM 8 doesn't support arm64_32.
                        //  We can use armv7k because they are compatible at bitcode level.
                        "arm64_32" -> "armv7k"
                        else -> targetTriple.architecture
                    },
                    os = "${targetTriple.os}$osVersionMin"
            )
            add(listOf("-target", targetArg.toString()))
        } else {
            add(listOf("-target", configurables.targetTriple.toString()))
        }
        val hasCustomSysroot = configurables is ZephyrConfigurables
                || configurables is WasmConfigurables
                || configurables is AndroidConfigurables
        if (!hasCustomSysroot) {
            when (configurables) {
                // isysroot and sysroot on darwin are _almost_ synonyms.
                // The first one parses SDKSettings.json while second one is not.
                is AppleConfigurables -> add(listOf("-isysroot", absoluteTargetSysRoot))
                else -> add(listOf("--sysroot=$absoluteTargetSysRoot"))
            }

        }
        // PIC is not required on Windows (and Clang will fail with `error: unsupported option '-fPIC'`)
        if (configurables !is MingwConfigurables) {
            // `-fPIC` allows us to avoid some problems when producing dynamic library.
            // See KT-43502.
            add(listOf("-fPIC"))
        }
    }.flatten()

    private val specificClangArgs: List<String> = when (target) {
        KonanTarget.LINUX_ARM32_HFP -> listOf(
                "-mfpu=vfp", "-mfloat-abi=hard"
        )

        KonanTarget.ANDROID_ARM32, KonanTarget.ANDROID_ARM64,
        KonanTarget.ANDROID_X86, KonanTarget.ANDROID_X64 -> {
            val clangTarget = targetTriple.withoutVendor()
            val architectureDir = Android.architectureDirForTarget(target)
            val toolchainSysroot = "$absoluteTargetToolchain/sysroot"
            listOf(
                    "-D__ANDROID_API__=${Android.API}",
                    "--sysroot=$absoluteTargetSysRoot/$architectureDir",
                    "-I$toolchainSysroot/usr/include/c++/v1",
                    "-I$toolchainSysroot/usr/include",
                    "-I$toolchainSysroot/usr/include/$clangTarget"
            )
        }

        // By default WASM target forces `hidden` visibility which causes linkage problems.
        KonanTarget.WASM32 -> listOf(
                    "-fno-rtti",
                    "-fno-exceptions",
                    "-fvisibility=default",
                    "-D_LIBCPP_ABI_VERSION=2",
                    "-D_LIBCPP_NO_EXCEPTIONS=1",
                    "-nostdinc",
                    "-Xclang", "-nobuiltininc",
                    "-Xclang", "-nostdsysteminc",
                    "-Xclang", "-isystem$absoluteTargetSysRoot/include/libcxx",
                    "-Xclang", "-isystem$absoluteTargetSysRoot/lib/libcxxabi/include",
                    "-Xclang", "-isystem$absoluteTargetSysRoot/include/compat",
                    "-Xclang", "-isystem$absoluteTargetSysRoot/include/libc"
        )

        is KonanTarget.ZEPHYR -> listOf(
                "-fno-rtti",
                "-fno-exceptions",
                "-fno-asynchronous-unwind-tables",
                "-fno-pie",
                "-fno-pic",
                "-fshort-enums",
                "-nostdinc",
                // TODO: make it a libGcc property?
                // We need to get rid of wasm sysroot first.
                "-isystem ${configurables.targetToolchain}/../lib/gcc/arm-none-eabi/7.2.1/include",
                "-isystem ${configurables.targetToolchain}/../lib/gcc/arm-none-eabi/7.2.1/include-fixed",
                "-isystem$absoluteTargetSysRoot/include/libcxx",
                "-isystem$absoluteTargetSysRoot/include/libc"
        ) + (configurables as ZephyrConfigurables).constructClangArgs()

        else -> emptyList()
    }

    val clangPaths = listOf("$absoluteLlvmHome/bin", binDir)

    private val jdkDir by lazy {
        val home = File.javaHome.absoluteFile
        if (home.child("include").exists)
            home.absolutePath
        else
            home.parentFile.absolutePath
    }

    val hostCompilerArgsForJni = listOf("", HostManager.jniHostPlatformIncludeDir).map { "-I$jdkDir/include/$it" }.toTypedArray()

    /**
     * Clang args for Objectice-C and plain C compilation.
     */
    val clangArgs: Array<String> = (commonClangArgs + specificClangArgs).toTypedArray()

    /**
     * Clang args for C++ compilation.
     */
    val clangXXArgs: Array<String> = clangArgs + when (configurables) {
        is AppleConfigurables -> arrayOf(
                "-stdlib=libc++",
                // Starting from Xcode 12.5, platform SDKs contain C++ stdlib.
                // It results in two c++ stdlib in search path (one from LLVM, another from SDK).
                // We workaround this problem by explicitly specifying path to stdlib.
                // TODO: Revise after LLVM update.
                "-nostdinc++", "-isystem", "$absoluteLlvmHome/include/c++/v1"
        )
        else -> emptyArray()
    }

    val clangArgsForKonanSources =
            clangXXArgs + clangArgsSpecificForKonanSources

    private val libclangSpecificArgs =
            // libclang works not exactly the same way as the clang binary and
            // (in particular) uses different default header search path.
            // See e.g. http://lists.llvm.org/pipermail/cfe-dev/2013-November/033680.html
            // We workaround the problem with -isystem flag below.
            // TODO: Revise after update to LLVM 10.
            listOf("-isystem", "$absoluteLlvmHome/lib/clang/${configurables.llvmVersion}/include")

    /**
     * libclang args for plain C and Objective-C.
     *
     * Note that it's different from [clangArgs].
     */
    val libclangArgs: List<String> =
            libclangSpecificArgs + clangArgs

    /**
     * libclang args for C++.
     *
     * Note that it's different from [clangXXArgs].
     */
    val libclangXXArgs: List<String> =
            libclangSpecificArgs + clangXXArgs

    private val targetClangCmd
            = listOf("${absoluteLlvmHome}/bin/clang") + clangArgs

    private val targetClangXXCmd
            = listOf("${absoluteLlvmHome}/bin/clang++") + clangXXArgs

    private val targetArCmd
            = listOf("${absoluteLlvmHome}/bin/llvm-ar")


    fun clangC(vararg userArgs: String) = targetClangCmd + userArgs.asList()

    fun clangCXX(vararg userArgs: String) = targetClangXXCmd + userArgs.asList()

    fun llvmAr(vararg userArgs: String) = targetArCmd + userArgs.asList()
}

