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

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

// Use "clang -v -save-temps" to write linkCommand() method 
// for another implementation of this class.
abstract class LinkerFlags(val configurables: Configurables)
   /* : Configurables by configurables */{

    protected val llvmBin = "${configurables.absoluteLlvmHome}/bin"
    protected val llvmLib = "${configurables.absoluteLlvmHome}/lib"

    private val libLTODir = when (HostManager.host) {
        KonanTarget.MACBOOK, KonanTarget.LINUX -> llvmLib
        KonanTarget.MINGW -> llvmBin
        else -> error("Don't know libLTO location for this platform.")
    }

    val libLTO = "$libLTODir/${System.mapLibraryName("LTO")}"

    val targetLibffi = configurables.libffiDir ?.let { listOf("${configurables.absoluteLibffiDir}/lib/libffi.a") } ?: emptyList()

    open val useCompilerDriverAsLinker: Boolean get() = false // TODO: refactor.

    abstract fun linkCommand(objectFiles: List<ObjectFile>,
                             executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): Command

    open fun linkCommandSuffix(): List<String> = emptyList()

    abstract fun filterStaticLibraries(binaries: List<String>): List<String> 

    open fun linkStaticLibraries(binaries: List<String>): List<String> {
        val libraries = filterStaticLibraries(binaries)
        // Let's just pass them as absolute paths
        return libraries
    }
}

open class AndroidLinker(targetProperties: AndroidConfigurables)
    : LinkerFlags(targetProperties), AndroidConfigurables by targetProperties  {

    private val prefix = "$absoluteTargetToolchain/bin/"
    private val clang = "$prefix/clang"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): Command {
        // liblog.so must be linked in, as we use its functionality in runtime.
        return Command(clang).apply {
            + "-o"
            + executable
            + "-fPIC"
            + "-shared"
            + "-llog"
            + objectFiles
            if (optimize) + linkerOptimizationFlags
            if (!debug) + linkerNoDebugFlags
            if (dynamic) + linkerDynamicFlags
            + linkerKonanFlags
        }
    }
}

open class MacOSBasedLinker(targetProperties: AppleConfigurables)
    : LinkerFlags(targetProperties), AppleConfigurables by targetProperties {

    private val linker = "$absoluteTargetToolchain/usr/bin/ld"
    internal val dsymutil = "$absoluteLlvmHome/bin/llvm-dsymutil"

    open val osVersionMinFlags: List<String> by lazy {
        listOf(
                osVersionMinFlagLd,
                osVersionMin + ".0")
    }

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): Command {
        return object : Command(linker) {} .apply {
            + "-demangle"
            + listOf("-object_path_lto", "temporary.o", "-lto_library", libLTO)
            + listOf("-dynamic", "-arch", arch)
            + osVersionMinFlags
            + listOf("-syslibroot", absoluteTargetSysRoot, "-o", executable)
            + objectFiles
            if (optimize) + linkerOptimizationFlags
            if (!debug) + linkerNoDebugFlags
            if (dynamic) + linkerDynamicFlags
            + linkerKonanFlags
            + "-lSystem"
        }
    }

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

    open fun dsymutilCommand(executable: ExecutableFile, outputDsymBundle: String): List<String> = 
        listOf(dsymutil, executable, "-o", outputDsymBundle)

    open fun dsymutilDryRunVerboseCommand(executable: ExecutableFile): List<String> =
            listOf(dsymutil, "-dump-debug-map" ,executable)
}

open class LinuxBasedLinker(targetProperties: LinuxBasedConfigurables)
    : LinkerFlags(targetProperties), LinuxBasedConfigurables by targetProperties {

    override val libGcc: String = "$absoluteTargetSysRoot/${super.libGcc}"
    private val linker = "$absoluteTargetToolchain/bin/ld.gold"
    private val specificLibs
        = abiSpecificLibraries.map { "-L${absoluteTargetSysRoot}/$it" }

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): Command {
        val isMips = (configurables is LinuxMIPSConfigurables)

        // TODO: Can we extract more to the konan.configurables?
        return Command(linker).apply {
            + "--sysroot=${absoluteTargetSysRoot}"
            + "-export-dynamic"
            + "-z"
            + "relro"
            + "--build-id"
            + "--eh-frame-hdr"
            + "-dynamic-linker"
            + dynamicLinker
            + "-o"
            + executable
            if (!dynamic) + "$absoluteTargetSysRoot/usr/lib64/crt1.o"
            + "$absoluteTargetSysRoot/usr/lib64/crti.o"
            if (dynamic)
                + "$libGcc/crtbeginS.o"
            else
                + "$libGcc/crtbegin.o"
            + "-L$llvmLib"
            + "-L$libGcc"
            if (!isMips) + "--hash-style=gnu" // MIPS doesn't support hash-style=gnu
            + specificLibs
            + listOf("-L$absoluteTargetSysRoot/../lib", "-L$absoluteTargetSysRoot/lib", "-L$absoluteTargetSysRoot/usr/lib")
            if (optimize) {
                + "-plugin"
                +"$llvmLib/LLVMgold.so"
                + pluginOptimizationFlags
            }
            if (optimize) + linkerOptimizationFlags
            if (!debug) + linkerNoDebugFlags
            if (dynamic) + linkerDynamicFlags
            + objectFiles
            + linkerKonanFlags
            + listOf("-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed",
                    "-lc", "-lgcc", "--as-needed", "-lgcc_s", "--no-as-needed")
            if (dynamic)
                + "$libGcc/crtendS.o"
            else
                + "$libGcc/crtend.o"
            + "$absoluteTargetSysRoot/usr/lib64/crtn.o"
        }
    }
}

open class MingwLinker(targetProperties: MingwConfigurables)
    : LinkerFlags(targetProperties), MingwConfigurables by targetProperties {

    private val linker = "$absoluteTargetToolchain/bin/clang++"

    override val useCompilerDriverAsLinker: Boolean get() = true

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter { it.isWindowsStaticLib || it.isUnixStaticLib }

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): Command {
        return Command(linker).apply {
            + listOf("-o", executable)
            + objectFiles
            if (optimize) + linkerOptimizationFlags
            if (!debug)  + linkerNoDebugFlags
            if (dynamic) + linkerDynamicFlags
        }
    }

    override fun linkCommandSuffix() = linkerKonanFlags
}

open class WasmLinker(targetProperties: WasmConfigurables)
    : LinkerFlags(targetProperties), WasmConfigurables by targetProperties {

    private val clang = "clang"

    override val useCompilerDriverAsLinker: Boolean get() = false

    override fun filterStaticLibraries(binaries: List<String>) 
        = binaries.filter{it.isJavaScript}

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): Command {
        return object: Command("") {
            override fun execute() {
                val src = File(objectFiles.single())
                val dst = File(executable)
                src.recursiveCopyTo(dst)
                javaScriptLink(args, executable)
            }

            private fun javaScriptLink(jsFiles: List<String>, executable: String): String {
                val linkedJavaScript = File("$executable.js")

                val linkerHeader = "var konan = { libraries: [] };\n"
                val linkerFooter = """|if (isBrowser()) {
                                      |   konan.moduleEntry([]);
                                      |} else {
                                      |   konan.moduleEntry(arguments);
                                      |}""".trimMargin()

                linkedJavaScript.writeBytes(linkerHeader.toByteArray());

                jsFiles.forEach {
                    linkedJavaScript.appendBytes(File(it).readBytes())
                }

                linkedJavaScript.appendBytes(linkerFooter.toByteArray());
                return linkedJavaScript.name
            }
        }
    }
}

open class ZephyrLinker(targetProperties: ZephyrConfigurables)
    : LinkerFlags(targetProperties), ZephyrConfigurables by targetProperties {

    private val linker = "$absoluteTargetToolchain/bin/ld"

    override val useCompilerDriverAsLinker: Boolean get() = false

    override fun filterStaticLibraries(binaries: List<String>)
        = emptyList<String>()

    override fun linkCommand(objectFiles: List<ObjectFile>, executable: ExecutableFile, optimize: Boolean, debug: Boolean, dynamic: Boolean): Command {
        return Command(linker).apply {
            + listOf("-r", "--gc-sections", "--entry", "main")
            + listOf("-o", executable)
            + objectFiles
        }
    }
}

fun linker(configurables: Configurables): LinkerFlags  =
    when (configurables.target) {
        KonanTarget.LINUX, KonanTarget.RASPBERRYPI ->
            LinuxBasedLinker(configurables as LinuxConfigurables)
        KonanTarget.LINUX_MIPS32, KonanTarget.LINUX_MIPSEL32 ->
            LinuxBasedLinker(configurables as LinuxMIPSConfigurables)
        KonanTarget.MACBOOK, KonanTarget.IPHONE, KonanTarget.IPHONE_SIM ->
            MacOSBasedLinker(configurables as AppleConfigurables)
        KonanTarget.ANDROID_ARM32, KonanTarget.ANDROID_ARM64 ->
            AndroidLinker(configurables as AndroidConfigurables)
        KonanTarget.MINGW ->
            MingwLinker(configurables as MingwConfigurables)
        KonanTarget.WASM32 ->
            WasmLinker(configurables as WasmConfigurables)
        is KonanTarget.ZEPHYR ->
            ZephyrLinker(configurables as ZephyrConfigurables)
    }

