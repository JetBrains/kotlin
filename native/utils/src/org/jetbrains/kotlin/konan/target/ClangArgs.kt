/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.KonanExternalToolFailure
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

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

sealed class ClangArgs(
    private val configurables: Configurables,
    private val forJni: Boolean,
) {

    private val absoluteTargetToolchain = configurables.absoluteTargetToolchain
    private val absoluteTargetSysRoot = configurables.absoluteTargetSysRoot
    private val absoluteLlvmHome = configurables.absoluteLlvmHome
    val target = configurables.target
    private val targetTriple = configurables.targetTriple

    // TODO: Should be dropped in favor of real MSVC target.
    private val argsForWindowsJni = forJni && target == KonanTarget.MINGW_X64

    private val clangArgsSpecificForKonanSources : List<String>
        get() {
            val konanOptions = listOfNotNull(
                    target.architecture.name.takeIf { target != KonanTarget.WATCHOS_ARM64 },
                    "ARM32".takeIf { target == KonanTarget.WATCHOS_ARM64 },
                    target.family.name.takeIf { target.family != Family.MINGW },
                    "WINDOWS".takeIf { target.family == Family.MINGW },
                    "MACOSX".takeIf { target.family == Family.OSX },

                    "NO_64BIT_ATOMIC".takeUnless { target.supports64BitAtomics() },
                    "NO_UNALIGNED_ACCESS".takeUnless { target.supportsUnalignedAccess() },
                    "FORBID_BUILTIN_MUL_OVERFLOW".takeUnless { target.supports64BitMulOverflow() },

                    "OBJC_INTEROP".takeIf { target.supportsObjcInterop() },
                    "HAS_FOUNDATION_FRAMEWORK".takeIf { target.hasFoundationFramework() },
                    "HAS_UIKIT_FRAMEWORK".takeIf { target.hasUIKitFramework() },
                    "REPORT_BACKTRACE_TO_IOS_CRASH_LOG".takeIf { target.supportsIosCrashLog() },
                    "NEED_SMALL_BINARY".takeIf { target.needSmallBinary() },
                    "SUPPORTS_GRAND_CENTRAL_DISPATCH".takeIf { target.supportsGrandCentralDispatch },
                    "SUPPORTS_SIGNPOSTS".takeIf { target.supportsSignposts },
            ).map { "KONAN_$it=1" }
            val otherOptions = listOfNotNull(
                    "USE_ELF_SYMBOLS=1".takeIf { target.binaryFormat() == BinaryFormat.ELF },
                    "ELFSIZE=${target.pointerBits()}".takeIf { target.binaryFormat() == BinaryFormat.ELF },
                    "MACHSIZE=${target.pointerBits()}".takeIf { target.binaryFormat() == BinaryFormat.MACH_O },
                    "__ANDROID__".takeIf { target.family == Family.ANDROID },
                    "USE_PE_COFF_SYMBOLS=1".takeIf { target.binaryFormat() == BinaryFormat.PE_COFF },
                    "UNICODE".takeIf { target.family == Family.MINGW },
                    "USE_WINAPI_UNWIND=1".takeIf { target.supportsWinAPIUnwind() },
                    "USE_GCC_UNWIND=1".takeIf { target.supportsGccUnwind() }
            )
            return (konanOptions + otherOptions).map { "-D$it" }
        }

    private val binDir = "$absoluteTargetToolchain/bin"
    // TODO: Use buildList
    private val commonClangArgs: List<String> = mutableListOf<List<String>>().apply {
        // Currently, MinGW toolchain contains old LLVM 8, and -fuse-ld=lld picks linker from there.
        // And, unfortunately, `-fuse-ld=<absolute path>` doesn't work correctly for MSVC toolchain.
        // That's why we just don't add $absoluteTargetToolchain/bin to binary search path in case of JNI compilation.
        // TODO: Can be removed after MinGW sysroot update.
        if (!argsForWindowsJni) {
            add(listOf("-B$binDir"))
        } else {
            require(configurables is MingwConfigurables)
            add(configurables.msvc.compilerFlags())
            add(configurables.windowsKit.compilerFlags())
            // Do not depend on link.exe from Visual Studio.
            add(listOf("-fuse-ld=lld"))
        }
        add(listOf("-fno-stack-protector"))
        if (configurables is GccConfigurables) {
            add(listOf("--gcc-toolchain=${configurables.absoluteGccToolchain}"))
        }
        val targetString: String = when {
            argsForWindowsJni -> "x86_64-pc-windows-msvc"
            configurables is AppleConfigurables -> {
                targetTriple.copy(
                        os = "${targetTriple.os}${configurables.osVersionMin}"
                ).toString()
            }
            else -> configurables.targetTriple.toString()
        }
        add(listOf("-target", targetString))
        val hasCustomSysroot = configurables is ZephyrConfigurables
                || configurables is WasmConfigurables
                || configurables is AndroidConfigurables
                || argsForWindowsJni
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

       KonanTarget.WATCHOS_ARM32 -> listOf(
                // Force generation of ARM instruction set instead of Thumb-2.
                // It allows LLVM ARM backend to encode bigger offsets in BL instruction,
                // thus allowing to generate a slightly bigger binaries.
                // See KT-37368.
                "-marm"
        )

        KonanTarget.ANDROID_ARM32, KonanTarget.ANDROID_ARM64,
        KonanTarget.ANDROID_X86, KonanTarget.ANDROID_X64,
        -> {
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

        else -> emptyList()
    }

    val clangPaths = listOf("$absoluteLlvmHome/bin", binDir)

    /**
     * Clang args for Objectice-C and plain C compilation.
     */
    val clangArgs: Array<String> = (commonClangArgs + specificClangArgs).toTypedArray()

    /**
     * Clang args for C++ compilation.
     */
    val clangXXArgs: Array<String> = clangArgs + when (configurables) {
        is AppleConfigurables -> arrayOf(
                "-stdlib=libc++"
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

    /**
     * Should be used when compiling library for JNI.
     * For example, it is used for Kotlin/Native's Clang and LLVM libraries.
     */
    class Jni(configurables: Configurables) : ClangArgs(configurables, forJni = true) {
        private val jdkDir by lazy {
            val home = File.javaHome.absoluteFile
            if (home.child("include").exists)
                home.absolutePath
            else
                home.parentFile.absolutePath
        }

        val hostCompilerArgsForJni: Array<String> by lazy {
            listOf("", HostManager.jniHostPlatformIncludeDir)
                    .map { "-I$jdkDir/include/$it" }
                    .toTypedArray()
        }
    }

    /**
     * Used for compiling native code that meant to be run on end-user's hardware.
     * E.g., Kotlin/Native runtime and interop stubs.
     */
    class Native(configurables: Configurables) : ClangArgs(configurables, forJni = false)
}


fun prepareXcode16HacksIfNeeded(
    target: KonanTarget,
    temporaryRoot: java.io.File,
): List<String> {
    return emptyList()
    if (!HostManager.hostIsMac) return emptyList()
    if (!target.family.isAppleFamily) return emptyList()
    if (dumpXcodeVersion() < 16) return emptyList()

    temporaryRoot.mkdirs()

    val vfsOverlay = temporaryRoot.resolve("hack.yaml")
    val headersRoot = temporaryRoot.resolve("include")
    vfsOverlay.writeText(
        xcode16VfsOverlay(
            sdkRoot = dumpSdkPath(target).path,
            headersRoot = headersRoot.path + "/include",
        )
    )

    copyXcode16HeaderHacks(
        headersRoot = headersRoot,
    )

    return listOf("-ivfsoverlay", vfsOverlay.path)
}

fun xcode16VfsOverlay(
    sdkRoot: String,
    headersRoot: String,
): String = """
        {
          'case-sensitive': 'false',
          'roots': [
            {
              "contents": [
                { 'external-contents': "${headersRoot}/simd/packed.h", 'name': "simd/packed.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/types.h", 'name': "simd/types.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/quaternion.h", 'name': "simd/quaternion.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/matrix_types.h", 'name': "simd/matrix_types.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/matrix.h", 'name': "simd/matrix.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/conversion.h", 'name': "simd/conversion.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/vector_make.h", 'name': "simd/vector_make.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/common.h", 'name': "simd/common.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/logic.h", 'name': "simd/logic.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/simd.h", 'name': "simd/simd.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/vector_types.h", 'name': "simd/vector_types.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/math.h", 'name': "simd/math.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/extern.h", 'name': "simd/extern.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/vector.h", 'name': "simd/vector.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/geometry.h", 'name': "simd/geometry.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/simd/base.h", 'name': "simd/base.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/math.h", 'name': "math.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/tgmath.h", 'name': "tgmath.h", 'type': 'file' },
                { 'external-contents': "${headersRoot}/empty", 'name': "libxml2/libxml/module.modulemap", 'type': 'file' }
              ],
              'name': "${sdkRoot}/usr/include",
              'type': 'directory'
            },
          ],
          'version': 0,
        }
    """.trimIndent()

fun copyXcode16HeaderHacks(headersRoot: java.io.File) {
    ZipInputStream(
        ClangArgs::class.java.getResourceAsStream("/headerHacks.zip")!!
    ).use { zip ->
        for (entry in generateSequence { zip.nextEntry }) {
            val file = headersRoot.resolve(entry.name)
            if (!entry.isDirectory) {
                file.parentFile.mkdirs()
                file.writeBytes(zip.readBytes())
            }
        }
    }
}

fun dumpSdkPath(target: KonanTarget): java.io.File {
    val sdk = when (target) {
        KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64 -> "macosx"
        KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.IOS_X64 -> "iphonesimulator"
        KonanTarget.IOS_ARM64 -> "iphoneos"
        KonanTarget.WATCHOS_X64, KonanTarget.WATCHOS_SIMULATOR_ARM64 -> "watchsimulator"
        KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64, KonanTarget.WATCHOS_DEVICE_ARM64 -> "watchos"
        KonanTarget.TVOS_ARM64 -> "appletvos"
        KonanTarget.TVOS_X64, KonanTarget.TVOS_SIMULATOR_ARM64 -> "appletvsimulator"
        else -> throw Exception("dumpSdkPath: ${target}")
    }
    return java.io.File(Command("xcrun", "--sdk", sdk, "--show-sdk-path").getOutputLines().first())
}

// FIXME: Command line tools
fun dumpXcodePath(): java.io.File = java.io.File(Command("/usr/bin/xcode-select", "-p").getOutputLines().first())
fun dumpXcodeVersion(): Int {
    val xcodePath = dumpXcodePath()
    val versionPlist = xcodePath.parentFile.resolve("version.plist")
    if (!versionPlist.exists()) {
        throw KonanExternalToolFailure("version.plist: ${xcodePath}", "xcode-select")
    }

    val xcodeVersion = Command(
        "/usr/libexec/PlistBuddy", versionPlist.canonicalPath,
        "-c", "Print :CFBundleShortVersionString",
    ).getOutputLines().first()

    return xcodeVersion.split(".")[0].toInt()
}
