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

class ClangArgs(private val configurables: Configurables) : Configurables by configurables {

    val targetArg = if (configurables is NonAppleConfigurables)
        configurables.targetArg
    else null

    val specificClangArgs: List<String>
        get() {
            val result = when (target) {
                KonanTarget.LINUX ->
                    listOf("--sysroot=$absoluteTargetSysRoot")
                KonanTarget.RASPBERRYPI ->
                    listOf("-target", targetArg!!,
                            "-mfpu=vfp", "-mfloat-abi=hard",
                            "--sysroot=$absoluteTargetSysRoot",
                            // TODO: those two are hacks.
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.8.3",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.8.3/arm-linux-gnueabihf")

                KonanTarget.LINUX_MIPS32 ->
                    listOf("-target", targetArg!!,
                            "--sysroot=$absoluteTargetSysRoot",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.4",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.4/mips-unknown-linux-gnu")

                KonanTarget.LINUX_MIPSEL32 ->
                    listOf("-target", targetArg!!,
                            "--sysroot=$absoluteTargetSysRoot",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.4",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.4/mipsel-unknown-linux-gnu")

                KonanTarget.MINGW ->
                    listOf("-target", targetArg!!, "--sysroot=$absoluteTargetSysRoot", "-Xclang", "-flto-visibility-public-std")

                KonanTarget.MACBOOK ->
                    listOf("--sysroot=$absoluteTargetSysRoot", "-mmacosx-version-min=10.11")

                KonanTarget.IPHONE ->
                    listOf("-stdlib=libc++", "-arch", "arm64", "-isysroot", absoluteTargetSysRoot, "-miphoneos-version-min=8.0.0")

                KonanTarget.IPHONE_SIM ->
                    listOf("-stdlib=libc++", "-isysroot", absoluteTargetSysRoot, "-miphoneos-version-min=8.0.0")

                KonanTarget.ANDROID_ARM32 ->
                    listOf("-target", targetArg!!,
                            "--sysroot=$absoluteTargetSysRoot",
                            // HACKS!
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.x",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.x/arm-linux-androideabi")

                KonanTarget.ANDROID_ARM64 ->
                    listOf("-target", targetArg!!,
                            "--sysroot=$absoluteTargetSysRoot",
                            // HACKS!
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.x",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.x/aarch64-linux-android")

                KonanTarget.WASM32 ->
                    listOf("-target", targetArg!!, "-fno-rtti", "-fno-exceptions",
                            "-D_LIBCPP_ABI_VERSION=2", "-D_LIBCPP_NO_EXCEPTIONS=1",
                            "-nostdinc", "-Xclang", "-nobuiltininc", "-Xclang", "-nostdsysteminc",
                            "-Xclang", "-isystem$absoluteTargetSysRoot/include/libcxx", "-Xclang", "-isystem$absoluteTargetSysRoot/lib/libcxxabi/include",
                            "-Xclang", "-isystem$absoluteTargetSysRoot/include/compat", "-Xclang", "-isystem$absoluteTargetSysRoot/include/libc")

                is KonanTarget.ZEPHYR ->
                    listOf("-target", targetArg!!,
                        "-fno-rtti",
                        "-fno-exceptions",
                        "-fno-asynchronous-unwind-tables",
                        "-fno-pie",
                        "-fno-pic",
                        "-nostdinc",
                        "-isystem$absoluteTargetSysRoot/include/libcxx",
                        "-isystem$absoluteTargetSysRoot/lib/libcxxabi/include",
                        "-isystem$absoluteTargetSysRoot/include/compat",
                        "-isystem$absoluteTargetSysRoot/include/libc") +
                    (configurables as ZephyrConfigurables).boardSpecificClangFlags

            }
            return result
        }

    val clangArgsSpecificForKonanSources
        get() = when (target) {
            KonanTarget.LINUX ->
                listOf("-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=64")

            KonanTarget.RASPBERRYPI ->
                listOf("-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=32")

            KonanTarget.LINUX_MIPS32 ->
                listOf("-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=32")

            KonanTarget.LINUX_MIPSEL32 ->
                listOf("-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=32")

            KonanTarget.MINGW ->
                listOf("-DUSE_GCC_UNWIND=1", "-DUSE_PE_COFF_SYMBOLS=1", "-DKONAN_WINDOWS=1", "-DKONAN_NO_MEMMEM=1")

            KonanTarget.MACBOOK ->
                listOf("-DKONAN_OSX=1", "-DKONAN_OBJC_INTEROP=1")

            KonanTarget.IPHONE ->
                listOf("-DKONAN_OBJC_INTEROP=1")

            KonanTarget.IPHONE_SIM ->
                listOf("-DKONAN_OBJC_INTEROP=1")

            KonanTarget.ANDROID_ARM32 ->
                listOf("-D__ANDROID__", "-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=32", "-DKONAN_ANDROID")

            KonanTarget.ANDROID_ARM64 ->
                listOf("-D__ANDROID__", "-DUSE_GCC_UNWIND=1", "-DUSE_ELF_SYMBOLS=1", "-DELFSIZE=64", "-DKONAN_ANDROID")

            KonanTarget.WASM32 ->
                listOf("-DKONAN_WASM=1", "-DKONAN_NO_FFI=1", "-DKONAN_NO_THREADS=1", "-DKONAN_NO_EXCEPTIONS=1",
                        "-DKONAN_NO_MATH=1", "-DKONAN_INTERNAL_DLMALLOC=1", "-DKONAN_INTERNAL_SNPRINTF=1",
                        "-DKONAN_INTERNAL_NOW=1", "-DKONAN_NO_MEMMEM")

            is KonanTarget.ZEPHYR ->
                listOf( "-DKONAN_ZEPHYR=1", "-DKONAN_NO_FFI=1", "-DKONAN_NO_THREADS=1", "-DKONAN_NO_EXCEPTIONS=1",
                        "-DKONAN_NO_MATH=1", "-DKONAN_INTERNAL_SNPRINTF=1", "-DKONAN_INTERNAL_NOW=1",
                        "-DKONAN_NO_MEMMEM=1")
        }

    private val host = HostManager.host

    private val binDir = when (host) {
        KonanTarget.LINUX -> "$absoluteTargetToolchain/bin"
        KonanTarget.MINGW -> "$absoluteTargetToolchain/bin"
        KonanTarget.MACBOOK -> "$absoluteTargetToolchain/usr/bin"
        else -> throw TargetSupportException("Unexpected host platform")
    }

    private val extraHostClangArgs =
            if (configurables is LinuxBasedConfigurables) {
                listOf("--gcc-toolchain=${configurables.absoluteGccToolchain}")
            } else {
                emptyList()
            }

    val commonClangArgs = listOf("-B$binDir") + extraHostClangArgs

    val clangPaths = listOf("$absoluteLlvmHome/bin", binDir)

    private val jdkDir by lazy {
        val home = File.javaHome.absoluteFile
        if (home.child("include").exists)
            home.absolutePath
        else
            home.parentFile.absolutePath
    }

    val hostCompilerArgsForJni = listOf("", HostManager.jniHostPlatformIncludeDir).map { "-I$jdkDir/include/$it" }.toTypedArray()

    val clangArgs = (commonClangArgs + specificClangArgs).toTypedArray()

    val clangArgsForKonanSources =
            clangArgs + clangArgsSpecificForKonanSources

    val targetLibclangArgs: List<String> =
            // libclang works not exactly the same way as the clang binary and
            // (in particular) uses different default header search path.
            // See e.g. http://lists.llvm.org/pipermail/cfe-dev/2013-November/033680.html
            // We workaround the problem with -isystem flag below.
            listOf("-isystem", "$absoluteLlvmHome/lib/clang/$llvmVersion/include", *clangArgs)

    val targetClangCmd
            = listOf("${absoluteLlvmHome}/bin/clang") + clangArgs

    val targetClangXXCmd
            = listOf("${absoluteLlvmHome}/bin/clang++") + clangArgs

    fun clangC(vararg userArgs: String) = targetClangCmd + userArgs.asList()

    fun clangCXX(vararg userArgs: String) = targetClangXXCmd + userArgs.asList()
}

