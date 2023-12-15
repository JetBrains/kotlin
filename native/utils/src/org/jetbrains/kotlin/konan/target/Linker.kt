/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.file.*

typealias ObjectFile = String
typealias ExecutableFile = String

enum class LinkerOutputKind {
    DYNAMIC_LIBRARY,
    STATIC_LIBRARY,
    EXECUTABLE
}

// Here we take somewhat unexpected approach - we create the thin
// library, and then repack it during post-link phase.
// This way we ensure .a inputs are properly processed.
private fun staticGnuArCommands(ar: String, executable: ExecutableFile,
                                objectFiles: List<ObjectFile>, libraries: List<String>) = when {
        HostManager.hostIsMingw -> {
            val temp = executable.replace('/', '\\') + "__"
            val arWindows = ar.replace('/', '\\')
            listOf(
                    Command(arWindows, "-rucT", temp).apply {
                        +objectFiles
                        +libraries
                    },
                    Command("cmd", "/c").apply {
                        +"(echo create $executable & echo addlib ${temp} & echo save & echo end) | $arWindows -M"
                    },
                    Command("cmd", "/c", "del", "/q", temp))
        }
        HostManager.hostIsLinux || HostManager.hostIsMac -> listOf(
                     Command(ar, "cqT", executable).apply {
                        +objectFiles
                        +libraries
                     },
                     Command("/bin/sh", "-c").apply {
                        +"printf 'create $executable\\naddlib $executable\\nsave\\nend' | $ar -M"
                     })
        else -> TODO("Unsupported host ${HostManager.host}")
    }

// Use "clang -v -save-temps" to write linkCommand() method 
// for another implementation of this class.
abstract class LinkerFlags(val configurables: Configurables) {

    protected val llvmBin = "${configurables.absoluteLlvmHome}/bin"

    open val useCompilerDriverAsLinker: Boolean get() = false // TODO: refactor.

    /**
     * Returns list of commands that produces final linker output.
     */
    // TODO: Number of arguments is quite big. Better to pass args via object.
    abstract fun finalLinkCommands(objectFiles: List<ObjectFile>, executable: ExecutableFile,
                                   libraries: List<String>, linkerArgs: List<String>,
                                   optimize: Boolean, debug: Boolean,
                                   kind: LinkerOutputKind, outputDsymBundle: String,
                                   mimallocEnabled: Boolean,
                                   sanitizer: SanitizerKind? = null): List<Command>

    /**
     * Returns list of commands that link object files into a single one.
     * Pre-linkage is useful for hiding dependency symbols.
     */
    open fun preLinkCommands(objectFiles: List<ObjectFile>, output: ObjectFile): List<Command> =
            error("Pre-link is unsupported for ${configurables.target}.")

    abstract fun filterStaticLibraries(binaries: List<String>): List<String>

    open fun linkStaticLibraries(binaries: List<String>): List<String> {
        val libraries = filterStaticLibraries(binaries)
        // Let's just pass them as absolute paths.
        return libraries
    }

    open fun provideCompilerRtLibrary(libraryName: String, isDynamic: Boolean = false): String? {
        System.err.println("Can't provide $libraryName.")
        return null
    }
}

class AndroidLinker(targetProperties: AndroidConfigurables)
    : LinkerFlags(targetProperties), AndroidConfigurables by targetProperties {

    private val clangTarget = when (val targetString = targetProperties.targetTriple.toString()) {
        "arm-unknown-linux-androideabi" -> "armv7a-linux-androideabi"
        else -> targetProperties.targetTriple.withoutVendor()
    }
    private val prefix = "$absoluteTargetToolchain/bin/${clangTarget}${Android.API}"
    private val clang = if (HostManager.hostIsMingw) "$prefix-clang.cmd" else "$prefix-clang"
    private val ar = "$absoluteTargetToolchain/${targetProperties.targetTriple.withoutVendor()}/bin/ar"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun filterStaticLibraries(binaries: List<String>) = binaries.filter { it.isUnixStaticLib }

    override fun finalLinkCommands(objectFiles: List<ObjectFile>, executable: ExecutableFile,
                                   libraries: List<String>, linkerArgs: List<String>,
                                   optimize: Boolean, debug: Boolean,
                                   kind: LinkerOutputKind, outputDsymBundle: String,
                                   mimallocEnabled: Boolean,
                                   sanitizer: SanitizerKind?): List<Command> {
        require(sanitizer == null) {
            "Sanitizers are unsupported"
        }
        if (kind == LinkerOutputKind.STATIC_LIBRARY)
            return staticGnuArCommands(ar, executable, objectFiles, libraries)

        val dynamic = kind == LinkerOutputKind.DYNAMIC_LIBRARY
        val toolchainSysroot = "${absoluteTargetToolchain}/sysroot"
        val architectureDir = Android.architectureDirForTarget(target)
        val apiSysroot = "$absoluteTargetSysRoot/$architectureDir"
        val clangTarget = targetTriple.withoutVendor()
        val libDirs = listOf(
                "--sysroot=$apiSysroot",
                if (target == KonanTarget.ANDROID_X64) "-L$apiSysroot/usr/lib64" else "-L$apiSysroot/usr/lib",
                "-L$toolchainSysroot/usr/lib/$clangTarget/${Android.API}",
                "-L$toolchainSysroot/usr/lib/$clangTarget")
        return listOf(Command(clang).apply {
            +"-o"
            +executable
            when (kind) {
                LinkerOutputKind.EXECUTABLE -> +listOf("-fPIE", "-pie")
                LinkerOutputKind.DYNAMIC_LIBRARY -> +listOf("-fPIC", "-shared")
                LinkerOutputKind.STATIC_LIBRARY -> {}
            }
            +"-target"
            +clangTarget
            +libDirs
            +objectFiles
            if (optimize) +linkerOptimizationFlags
            if (!debug) +linkerNoDebugFlags
            if (dynamic) +linkerDynamicFlags
            if (dynamic) +"-Wl,-soname,${File(executable).name}"
            +linkerKonanFlags
            if (mimallocEnabled) +mimallocLinkerDependencies
            +libraries
            +linkerArgs
        })
    }
}

class MacOSBasedLinker(targetProperties: AppleConfigurables)
    : LinkerFlags(targetProperties), AppleConfigurables by targetProperties {

    private val libtool = "$absoluteTargetToolchain/bin/libtool"
    private val linker = "$absoluteTargetToolchain/bin/ld"
    private val strip = "$absoluteTargetToolchain/bin/strip"
    private val dsymutil = "$absoluteTargetToolchain/bin/dsymutil"

    private val compilerRtDir: String? by lazy {
        val dir = File("$absoluteTargetToolchain/lib/clang/").listFiles.firstOrNull()?.absolutePath
        if (dir != null) "$dir/lib/darwin/" else null
    }

    override fun provideCompilerRtLibrary(libraryName: String, isDynamic: Boolean): String? {
        val prefix = when (target.family) {
            Family.IOS -> "ios"
            Family.WATCHOS -> "watchos"
            Family.TVOS -> "tvos"
            Family.OSX -> "osx"
            else -> error("Target $target is unsupported")
        }
        // Separate libclang_rt version for simulator appeared in Xcode 12.
        // We don't support Xcode versions older than 12.5 anymore, so no need to check Xcode version.
        val suffix = if (targetTriple.isSimulator) {
            "sim"
        } else {
            ""
        }

        val dir = compilerRtDir
        val mangledLibraryName = if (libraryName.isEmpty()) "" else "${libraryName}_"
        val extension = if (isDynamic) "_dynamic.dylib" else ".a"

        return if (dir != null) "$dir/libclang_rt.$mangledLibraryName$prefix$suffix$extension" else null
    }

    override fun filterStaticLibraries(binaries: List<String>) = binaries.filter { it.isUnixStaticLib }

    // Note that may break in case of 32-bit Mach-O. See KT-37368.
    override fun preLinkCommands(objectFiles: List<ObjectFile>, output: ObjectFile): List<Command> =
        Command(linker).apply {
            +"-r"
            +listOf("-arch", arch)
            +listOf("-syslibroot", absoluteTargetSysRoot)
            +objectFiles
            +listOf("-o", output)
        }.let(::listOf)

    /**
     * Construct -platform_version ld64 argument which contains info about
     * - SDK
     * - minimal OS version
     * - SDK version
     */
    private fun platformVersionFlags(): List<String> = mutableListOf<String>().apply {
        add("-platform_version")

        val platformName = when (target.family) {
            Family.OSX -> "macos"
            Family.IOS -> "ios"
            Family.TVOS -> "tvos"
            Family.WATCHOS -> "watchos"
            else -> error("Unexpected Apple target family: ${target.family}")
        } + if (targetTriple.isSimulator) "-simulator" else ""
        add(platformName)

        add("$osVersionMin.0")
        add(sdkVersion)
    }.toList()

    override fun finalLinkCommands(objectFiles: List<ObjectFile>, executable: ExecutableFile,
                                   libraries: List<String>, linkerArgs: List<String>,
                                   optimize: Boolean, debug: Boolean, kind: LinkerOutputKind,
                                   outputDsymBundle: String,
                                   mimallocEnabled: Boolean,
                                   sanitizer: SanitizerKind?): List<Command> {
        if (kind == LinkerOutputKind.STATIC_LIBRARY) {
            require(sanitizer == null) {
                "Sanitizers are unsupported"
            }
            return listOf(Command(libtool).apply {
                +"-static"
                +listOf("-o", executable)
                +objectFiles
                +libraries
            })
        }
        val dynamic = kind == LinkerOutputKind.DYNAMIC_LIBRARY

        val result = mutableListOf<Command>()

        result += Command(linker).apply {
            +"-demangle"
            +listOf("-dynamic", "-arch", arch)
            +platformVersionFlags()
            +listOf("-syslibroot", absoluteTargetSysRoot, "-o", executable)
            +objectFiles
            if (optimize) +linkerOptimizationFlags
            if (!debug) +linkerNoDebugFlags
            if (dynamic) +linkerDynamicFlags
            +linkerKonanFlags
            if (mimallocEnabled) +mimallocLinkerDependencies
            if (compilerRtLibrary != null) +compilerRtLibrary!!
            +libraries
            +linkerArgs
            +rpath(dynamic, sanitizer)
            when (sanitizer) {
                null -> {}
                SanitizerKind.ADDRESS -> +provideCompilerRtLibrary("asan", isDynamic=true)!!
                SanitizerKind.THREAD -> +provideCompilerRtLibrary("tsan", isDynamic=true)!!
            }
        }

        // TODO: revise debug information handling.
        if (debug) {
            result += dsymUtilCommand(executable, outputDsymBundle)
            if (optimize) {
                result += Command(strip, *stripFlags.toTypedArray(), executable)
            }
        }

        return result
    }

    private val compilerRtLibrary: String? by lazy {
        provideCompilerRtLibrary("")
    }

    private fun rpath(dynamic: Boolean, sanitizer: SanitizerKind?): List<String> = listOfNotNull(
            when (target.family) {
                Family.OSX -> "@executable_path/../Frameworks"
                Family.IOS,
                Family.WATCHOS,
                Family.TVOS -> "@executable_path/Frameworks"
                else -> error(target)
            },
            "@loader_path/Frameworks".takeIf { dynamic },
            compilerRtDir.takeIf { sanitizer != null }
    ).flatMap { listOf("-rpath", it) }

    fun dsymUtilCommand(executable: ExecutableFile, outputDsymBundle: String) =
            object : Command(dsymutilCommand(executable, outputDsymBundle)) {
                override fun runProcess(): Int =
                        executeCommandWithFilter(command)
            }

    // TODO: consider introducing a better filtering directly in Command.
    private fun executeCommandWithFilter(command: List<String>): Int {
        val builder = ProcessBuilder(command)

        // Inherit main process output streams.
        val isDsymUtil = (command[0] == dsymutil)

        builder.redirectOutput(Redirect.INHERIT)
        builder.redirectInput(Redirect.INHERIT)
        if (!isDsymUtil)
            builder.redirectError(Redirect.INHERIT)

        val process = builder.start()
        if (isDsymUtil) {
            /**
             * llvm-lto has option -alias that lets tool to know which symbol we use instead of _main,
             * llvm-dsym doesn't have such a option, so we ignore annoying warning manually.
             */
            val errorStream = process.errorStream
            val outputStream = bufferedReader(errorStream)
            while (true) {
                val line = outputStream.readLine() ?: break
                if (!line.contains("warning: could not find object file symbol for symbol _main"))
                    System.err.println(line)
            }
            outputStream.close()
        }
        val exitCode = process.waitFor()
        return exitCode
    }

    fun dsymutilCommand(executable: ExecutableFile, outputDsymBundle: String): List<String> =
            listOf(dsymutil, executable, "-o", outputDsymBundle)

    fun dsymutilDryRunVerboseCommand(executable: ExecutableFile): List<String> =
            listOf(dsymutil, "-dump-debug-map", executable)
}

class GccBasedLinker(targetProperties: GccConfigurables)
    : LinkerFlags(targetProperties), GccConfigurables by targetProperties {

    private val ar = if (HostManager.hostIsLinux) {
        "$absoluteTargetToolchain/bin/ar"
    } else {
        "$absoluteTargetToolchain/bin/llvm-ar"
    }
    override val libGcc = "$absoluteTargetSysRoot/${super.libGcc}"

    private val specificLibs = abiSpecificLibraries.map { "-L${absoluteTargetSysRoot}/$it" }

    override fun provideCompilerRtLibrary(libraryName: String, isDynamic: Boolean): String? {
        require(!isDynamic) {
            "Dynamic compiler rt librares are unsupported"
        }
        val targetSuffix = when (target) {
            KonanTarget.LINUX_X64 -> "x86_64"
            else -> error("$target is not supported.")
        }
        val dir = File("$absoluteLlvmHome/lib/clang/").listFiles.firstOrNull()?.absolutePath
        return if (dir != null) "$dir/lib/linux/libclang_rt.$libraryName-$targetSuffix.a" else null
    }

    override fun filterStaticLibraries(binaries: List<String>) = binaries.filter { it.isUnixStaticLib }

    override fun finalLinkCommands(objectFiles: List<ObjectFile>, executable: ExecutableFile,
                                   libraries: List<String>, linkerArgs: List<String>,
                                   optimize: Boolean, debug: Boolean,
                                   kind: LinkerOutputKind, outputDsymBundle: String,
                                   mimallocEnabled: Boolean,
                                   sanitizer: SanitizerKind?): List<Command> {
        if (kind == LinkerOutputKind.STATIC_LIBRARY) {
            require(sanitizer == null) {
                "Sanitizers are unsupported"
            }
            return staticGnuArCommands(ar, executable, objectFiles, libraries)
        }
        val isMips = target == KonanTarget.LINUX_MIPS32 || target == KonanTarget.LINUX_MIPSEL32
        val dynamic = kind == LinkerOutputKind.DYNAMIC_LIBRARY
        val crtPrefix = "$absoluteTargetSysRoot/$crtFilesLocation"
        // TODO: Can we extract more to the konan.configurables?
        return listOf(Command(absoluteLinker).apply {
            +"--sysroot=${absoluteTargetSysRoot}"
            +"-export-dynamic"
            +"-z"
            +"relro"
            +"--build-id"
            +"--eh-frame-hdr"
            +"-dynamic-linker"
            +dynamicLinker
            linkerHostSpecificFlags.forEach { +it }
            +"-o"
            +executable
            if (!dynamic) +"$crtPrefix/crt1.o"
            +"$crtPrefix/crti.o"
            +if (dynamic) "$libGcc/crtbeginS.o" else "$libGcc/crtbegin.o"
            +"-L$libGcc"
            if (!isMips) +"--hash-style=gnu" // MIPS doesn't support hash-style=gnu
            +specificLibs
            if (optimize) +linkerOptimizationFlags
            if (!debug) +linkerNoDebugFlags
            if (dynamic) +linkerDynamicFlags
            +objectFiles
            +libraries
            +linkerArgs
            if (mimallocEnabled) +mimallocLinkerDependencies
            // See explanation about `-u__llvm_profile_runtime` here:
            // https://github.com/llvm/llvm-project/blob/21e270a479a24738d641e641115bce6af6ed360a/llvm/lib/Transforms/Instrumentation/InstrProfiling.cpp#L930
            +linkerKonanFlags
            +linkerGccFlags
            +if (dynamic) "$libGcc/crtendS.o" else "$libGcc/crtend.o"
            +"$crtPrefix/crtn.o"
            when (sanitizer) {
                null -> {}
                SanitizerKind.ADDRESS -> {
                    +"-lrt"
                    +provideCompilerRtLibrary("asan")!!
                    +provideCompilerRtLibrary("asan_cxx")!!
                }
                SanitizerKind.THREAD -> {
                    +"-lrt"
                    +provideCompilerRtLibrary("tsan")!!
                    +provideCompilerRtLibrary("tsan_cxx")!!
                }
            }
        })
    }
}

class MingwLinker(targetProperties: MingwConfigurables)
    : LinkerFlags(targetProperties), MingwConfigurables by targetProperties {

    // TODO: Maybe always use llvm-ar?
    private val ar = if (HostManager.hostIsMingw) {
        "$absoluteTargetToolchain/bin/ar"
    } else {
        "$absoluteLlvmHome/bin/llvm-ar"
    }
    private val clang = "$absoluteLlvmHome/bin/clang++"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun filterStaticLibraries(binaries: List<String>) = binaries.filter { it.isWindowsStaticLib || it.isUnixStaticLib }

    override fun provideCompilerRtLibrary(libraryName: String, isDynamic: Boolean): String? {
        require(!isDynamic) {
            "Dynamic compiler rt librares are unsupported"
        }
        val targetSuffix = when (target) {
            KonanTarget.MINGW_X64 -> "x86_64"
            else -> error("$target is not supported.")
        }
        val dir = File("$absoluteLlvmHome/lib/clang/").listFiles.firstOrNull()?.absolutePath
        return if (dir != null) "$dir/lib/windows/libclang_rt.$libraryName-$targetSuffix.a" else null
    }

    override fun finalLinkCommands(objectFiles: List<ObjectFile>, executable: ExecutableFile,
                                   libraries: List<String>, linkerArgs: List<String>,
                                   optimize: Boolean, debug: Boolean,
                                   kind: LinkerOutputKind, outputDsymBundle: String,
                                   mimallocEnabled: Boolean,
                                   sanitizer: SanitizerKind?): List<Command> {
        require(sanitizer == null) {
            "Sanitizers are unsupported"
        }
        if (kind == LinkerOutputKind.STATIC_LIBRARY)
            return staticGnuArCommands(ar, executable, objectFiles, libraries)

        val dynamic = kind == LinkerOutputKind.DYNAMIC_LIBRARY

        fun Command.constructLinkerArguments(
                additionalArguments: List<String> = listOf(),
                skipDefaultArguments: List<String> = listOf()
        ): Command = apply {
            +listOf("--sysroot", absoluteTargetSysRoot)
            +listOf("-target", targetTriple.toString())
            +listOf("-o", executable)
            +objectFiles
            // --gc-sections flag may affect profiling.
            // See https://clang.llvm.org/docs/SourceBasedCodeCoverage.html#drawbacks-and-limitations.
            // TODO: switching to lld may help.
            if (optimize) {
                // TODO: Can be removed after LLD update.
                //  See KT-48085.
                if (!dynamic) {
                    +linkerOptimizationFlags
                }
            }
            if (!debug) +linkerNoDebugFlags
            if (dynamic) +linkerDynamicFlags
            +libraries
            +linkerArgs
            +linkerKonanFlags.filterNot { it in skipDefaultArguments }
            if (mimallocEnabled) +mimallocLinkerDependencies
            +additionalArguments
        }

        return listOf(Command(clang).constructLinkerArguments(additionalArguments = listOf("-fuse-ld=$absoluteLinker")))
    }
}

class WasmLinker(targetProperties: WasmConfigurables)
    : LinkerFlags(targetProperties), WasmConfigurables by targetProperties {

    override val useCompilerDriverAsLinker: Boolean get() = false

    override fun filterStaticLibraries(binaries: List<String>) = binaries.filter { it.isJavaScript }

    override fun finalLinkCommands(objectFiles: List<ObjectFile>, executable: ExecutableFile,
                                   libraries: List<String>, linkerArgs: List<String>,
                                   optimize: Boolean, debug: Boolean,
                                   kind: LinkerOutputKind, outputDsymBundle: String,
                                   mimallocEnabled: Boolean,
                                   sanitizer: SanitizerKind?): List<Command> {
        if (kind != LinkerOutputKind.EXECUTABLE) throw Error("Unsupported linker output kind")
        require(sanitizer == null) {
            "Sanitizers are unsupported"
        }

        val linkage = Command("$llvmBin/wasm-ld").apply {
            +objectFiles
            +listOf("-o", executable)
            +lldFlags
        }

        // TODO(horsh): maybe rethink it.
        val jsBindingsGeneration = object : Command() {
            override fun execute() {
                javaScriptLink(libraries, executable)
            }

            private fun javaScriptLink(jsFiles: List<String>, executable: String): String {
                val linkedJavaScript = File("$executable.js")

                val linkerHeader = "var konan = { libraries: [] };\n"
                val linkerFooter = """|if (isBrowser()) {
                                      |   konan.moduleEntry([]);
                                      |} else {
                                      |   konan.moduleEntry(arguments);
                                      |}""".trimMargin()

                linkedJavaScript.writeText(linkerHeader)

                jsFiles.forEach {
                    linkedJavaScript.appendBytes(File(it).readBytes())
                }

                linkedJavaScript.appendBytes(linkerFooter.toByteArray())
                return linkedJavaScript.name
            }
        }
        return listOf(linkage, jsBindingsGeneration)
    }
}

open class ZephyrLinker(targetProperties: ZephyrConfigurables)
    : LinkerFlags(targetProperties), ZephyrConfigurables by targetProperties {

    private val linker = "$absoluteTargetToolchain/bin/ld"

    override val useCompilerDriverAsLinker: Boolean get() = false

    override fun filterStaticLibraries(binaries: List<String>) = emptyList<String>()

    override fun finalLinkCommands(objectFiles: List<ObjectFile>, executable: ExecutableFile,
                                   libraries: List<String>, linkerArgs: List<String>,
                                   optimize: Boolean, debug: Boolean,
                                   kind: LinkerOutputKind, outputDsymBundle: String,
                                   mimallocEnabled: Boolean,
                                   sanitizer: SanitizerKind?): List<Command> {
        if (kind != LinkerOutputKind.EXECUTABLE) throw Error("Unsupported linker output kind: $kind")
        require(sanitizer == null) {
            "Sanitizers are unsupported"
        }
        return listOf(Command(linker).apply {
            +listOf("-r", "--gc-sections", "--entry", "main")
            +listOf("-o", executable)
            +objectFiles
            +libraries
            +linkerArgs
        })
    }
}

fun linker(configurables: Configurables): LinkerFlags =
        when (configurables) {
            is GccConfigurables -> GccBasedLinker(configurables)
            is AppleConfigurables -> MacOSBasedLinker(configurables)
            is AndroidConfigurables-> AndroidLinker(configurables)
            is MingwConfigurables -> MingwLinker(configurables)
            is WasmConfigurables -> WasmLinker(configurables)
            is ZephyrConfigurables -> ZephyrLinker(configurables)
            else -> error("Unexpected target: ${configurables.target}")
        }

