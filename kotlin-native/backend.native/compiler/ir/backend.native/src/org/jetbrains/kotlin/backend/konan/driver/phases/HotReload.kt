/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.driver.phases

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.driver.NativeBackendPhaseContext
import org.jetbrains.kotlin.backend.konan.driver.utilities.CExportFiles
import org.jetbrains.kotlin.backend.konan.llvm.objc.createObjCExportConvertersModule
import org.jetbrains.kotlin.backend.konan.llvm.objc.patchObjCRuntimeModule
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.config.NativeConfigurationKeys
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.library.impl.javaFile
import java.io.File

/**
 * Hot-Reload works through split-compilation. The Kotlin/Native runtime provides the HotReload module
 * to support code changes on the fly, through LLVM ORC JIT / JITLink support.
 *
 * Split-compilation is needed because the current K/N approach is closed-world, and producing guest code
 * speeds up the whole compilation pipeline to improve devex.
 *
 * The architecture is described as it follows:
 * 1. Compile the `host`, an object file containing the hot-reload launcher (`Konan_main`),
 *    the HotReload module (ORC JIT setup), patched ObjC export classes, and ObjC export converters
 *    (WritableTypeInfo for String, collections).
 * 2. Compilation of `Bootstrap/Guest` code containing user code and platform library CInterop stubs.
 *    This object is loaded at runtime via JITLink.
 * 3. Re-use of existing stdlib cache and platform caches (linked into the host at link time).
 *
 * The compilation process is split in two passes:
 * 1. Generate user code into bootstrap/guest module (run codegen for user code only, stdlib types are
 *    imported as external). Platform library stubs are included here since they have no pre-compiled cache.
 * 2. Create host module by collecting and linking launcher + HotReload runtime bitcode modules,
 *    ObjC export classes, and ObjC export converters. No stdlib is included — it comes from cache.
 *
 * The linking phase, to create the final executable, combines:
 * `host.o + libstdlib-cache.a + platform caches + user library caches (force-loaded) + libKNHR.dylib`.
 *
 * Remember, only the user code and platform stubs are compiled as guest.
 * The host provides `main → Konan_main`, which initializes the runtime and loads bootstrap.o via JITLink.
 *
 */

sealed interface HotReloadCompilationOutput {
    val bootstrapBitcodeFile: File
    val dependenciesTrackingResult: DependenciesTrackingResult
}

/**
 * Intermediate output from hot reload module compilation.
 */
internal data class HotReloadModuleCompilationOutput(
        val hostBitcodeFile: File,
        override val bootstrapBitcodeFile: File,
        override val dependenciesTrackingResult: DependenciesTrackingResult,
) : HotReloadCompilationOutput

/**
 * Output from guest-only hot reload compilation (bootstrap-only).
 */
internal data class HotReloadGuestCompilationOutput(
        override val bootstrapBitcodeFile: File,
        override val dependenciesTrackingResult: DependenciesTrackingResult,
) : HotReloadCompilationOutput

private data class BootstrapCompilationMetadata(
        val bootstrapObject: File,
        val forceLoadCaches: List<String>,
        val jitCaches: List<String>,
        val resolvedCaches: ResolvedCacheBinaries
)

private const val MANIFEST_DEPS_FILE_EXTENSION: String = "cache-deps"

val NativeSecondStageCompilationConfig.isCompilingHostCode: Boolean
    get() = hotReloadHostMode && produce == CompilerOutputKind.PROGRAM

val NativeSecondStageCompilationConfig.isCompilingGuestCodeOnly: Boolean
    get() = hotReloadGuestMode && produce == CompilerOutputKind.PROGRAM

val NativeSecondStageCompilationConfig.isCompilingHotReloadFramework: Boolean
    get() = hotReloadHostMode && produce == CompilerOutputKind.FRAMEWORK

internal fun PhaseEngine<NativeGenerationState>.compileModuleForHotReload(
        userModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        hostBitcodeFile: File,
        bootstrapBitcodeFile: File,
        cExportFiles: CExportFiles?,
) {

    compileModuleForHotReloadGuest(userModule, irBuiltIns, bootstrapBitcodeFile, cExportFiles)

    // First, patch and serialize the ObjC module using the main context (where objCExport is available)
    // The patched module contains OutputBase, OutputBoolean, etc. (renamed from KotlinBase, etc.)
    val objcBitcodeFile = File.createTempFile("objc_patched", ".bc")
    var hasObjCModule = false
    try {
        val objcModule = patchObjCRuntimeModule(context)
        if (objcModule != null) {
            LLVMWriteBitcodeToFile(objcModule, objcBitcodeFile.canonicalPath)
            hasObjCModule = true
            LLVMDisposeModule(objcModule)
        } else {
            context.log { "warning :: ObjC module not available (no objCExport) for Hot-Reload split compilation" }
        }
    } catch (e: Exception) {
        context.log { "warning :: failed to patch ObjC module during Hot-Reload split compilation: ${e.message}" }
    }

    // Let's use a separate LLVM context for the host module (so we avoid conflicts with the main one)
    val hostLlvmContext = requireNotNull(LLVMContextCreate()) {
        "Could not create new LLVM context for host module in split compilation"
    }

    try {
        val hostModules = collectHostModulesForProgramHotReload(context, hostLlvmContext, context.config.runtimeLogs)
        val hostModule = LLVMModuleCreateWithNameInContext("host", hostLlvmContext)!!
        hostModules.forEach { mod ->
            val failed = LLVMLinkModules2(hostModule, mod)
            if (failed != 0) {
                error("Failed to link module `host` module")
            }
        }

        // Link the patched ObjC module (OutputBase, etc.) into host
        // This is critical because bootstrap.o references these as external symbols
        if (hasObjCModule && objcBitcodeFile.exists()) {
            memScoped {
                val bufRef = alloc<LLVMMemoryBufferRefVar>()
                val errorRef = allocPointerTo<ByteVar>()

                val res = LLVMCreateMemoryBufferWithContentsOfFile(objcBitcodeFile.canonicalPath, bufRef.ptr, errorRef.ptr)
                if (res != 0) {
                    context.log { "warning :: failed to load ObjC bitcode into host module: ${errorRef.value?.toKString()}" }
                } else {
                    val memoryBuffer = bufRef.value
                    try {
                        val moduleRef = alloc<LLVMModuleRefVar>()
                        val parseRes = LLVMParseBitcodeInContext2(hostLlvmContext, memoryBuffer, moduleRef.ptr)
                        if (parseRes != 0) {
                            context.log { "warning :: failed to parse ObjC bitcode for host compilation" }
                        } else {
                            val objcModuleInHostContext = moduleRef.value!!
                            val linkFailed = LLVMLinkModules2(hostModule, objcModuleInHostContext)
                            if (linkFailed != 0) {
                                context.log { "warning :: failed to link ObjC module into host" }
                            }
                        }
                    } finally {
                        LLVMDisposeMemoryBuffer(memoryBuffer)
                    }
                }
            }
        }

        // Link ObjC export converters module (WritableTypeInfo for String, collections).
        // stdlib-cache.a has zero-initialized WritableTypeInfo (built with isFinalBinary=false),
        // so converters like String→NSString are missing. This module provides them at EXTERNAL
        // linkage, which overrides the COMMON linkage zeros from the cache at link time.
        if (context.config.target.family.isAppleFamily) {
            val convertersModule = createObjCExportConvertersModule(hostLlvmContext)
            val failed = LLVMLinkModules2(hostModule, convertersModule)
            if (failed != 0) {
                error("failed to link ObjC export converters module into host")
            }
        }

        // Define entry `Konan_main` as entry-point from the hot-reload launcher.
        insertAliasToEntryPoint(context, hostModule)

        // Write host bitcode
        LLVMWriteBitcodeToFile(hostModule, hostBitcodeFile.canonicalPath)
        LLVMDisposeModule(hostModule)
    } finally {
        LLVMContextDispose(hostLlvmContext)
        objcBitcodeFile.delete()
    }
}

/**
 * Runs backend codegen for hot reload with library bitcode linking.
 *
 * This is similar to [runBackendCodegen] but:
 * - Skips the full [LinkBitcodeDependenciesPhase] (which includes runtime modules)
 * - Links only library bitcode (interop stubs like knifunptr_*) needed by platform libraries
 */
internal fun PhaseEngine<NativeGenerationState>.runBackendCodegenForHotReload(
        module: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        cExportFiles: CExportFiles?
) {
    // The C++ runtime (MM, GC, alloc) comes from libstdlib-cache.a at link time, so we don't include it in bootstrap.o.

    runCodegen(module, irBuiltIns)
    val generatedBitcodeFiles = if (context.config.produceCInterface) {
        require(cExportFiles != null)
        val input = CExportGenerateApiInput(
                context.context.cAdapterExportedElements!!,
                headerFile = cExportFiles.header,
                defFile = cExportFiles.def,
                cppAdapterFile = cExportFiles.cppAdapter
        )
        runAndMeasurePhase(CExportGenerateApiPhase, input)
        runAndMeasurePhase(CExportCompileAdapterPhase, CExportCompileAdapterInput(cExportFiles.cppAdapter, cExportFiles.bitcodeAdapter))
        listOf(cExportFiles.bitcodeAdapter)
    } else {
        emptyList()
    }
    runAndMeasurePhase(CStubsPhase)

    val llvmModule = context.llvm.module
    if (context.config.needCompilerVerification || context.config.configuration.getBoolean(NativeConfigurationKeys.VERIFY_BITCODE)) {
        runAndMeasurePhase(VerifyBitcodePhase, llvmModule)
    }
    if (context.shouldPrintBitCode()) {
        runAndMeasurePhase(PrintBitcodePhase, llvmModule)
    }

    // Link library bitcode (interop stubs like knifunptr_*) into bootstrap
    // This is needed for platform library interop to work
    linkLibraryBitcodeForHotReload(context, generatedBitcodeFiles)
}

internal fun <C : NativeBackendPhaseContext> PhaseEngine<C>.compileAndLinkForHotReload(
        hotReloadOutput: HotReloadModuleCompilationOutput,
        outputFiles: OutputFiles,
        temporaryFiles: TempFiles,
) {

    // Compile host bitcode to object file
    val hostObjectFile = temporaryFiles.create(outputFiles.outputName, ".host.o")
    runAndMeasurePhase(ObjectFilesPhase, ObjectFilesPhaseInput(hotReloadOutput.hostBitcodeFile, hostObjectFile.javaFile()))

    // This is the final bootstrap.o that will be loaded by JITLink
    // The bootstrap is not a temporary file (at least for now), since it needes to be loaded by the runtime to start the actual program
    val (bootstrapObjectFile, forceLoadCaches, jitCaches, resolvedCaches) =
            compileBootstrapObjectAndManifest(outputFiles, hotReloadOutput)

    // TODO: add frameworks with weak linking, since SKIA uses newer CoreGraphics API
    val weakFrameworks = if (context.config.target.family == Family.OSX) {
        listOf(
                "Foundation", "CoreFoundation", "CoreGraphics", "CoreText",
                "CoreServices", "AppKit", "Metal", "QuartzCore", "OpenGL",
                "Security", "SystemConfiguration", "IOKit",
        )
    } else {
        // iOS, tvOS, watchOS
        listOf(
                "Foundation", "CoreFoundation", "CoreGraphics", "CoreText",
                "UIKit", "Metal", "QuartzCore", "Security",
        )
    }
    val weakFrameworkFlags = weakFrameworks.flatMap { listOf("-Wl,-weak_framework,$it") }

    val configurables = context.config.platform.configurables

    val dedupDir = temporaryFiles.create("dedup", "").javaFile().also { it.mkdirs() }
    val forceLoadFlags = forceLoadCaches.map { archivePath ->
        val dedupArchive = deduplicateArchive(archivePath, dedupDir, context.config)
        "-Wl,-force_load,$dedupArchive"
    }

    val jitLinkerFlags = listOf(
            "-L${configurables.absoluteLlvmHome}/lib",
            "-Wl,-rpath,${configurables.absoluteLlvmHome}/lib",
            "-Wl,-undefined,dynamic_lookup",
            "-Wl,-export_dynamic",
    ) + weakFrameworkFlags + forceLoadFlags + configurables.llvmJitLibs

    val existingLinkerArgs = context.config.configuration.getList(NativeConfigurationKeys.LINKER_ARGS)
    context.config.configuration.put(NativeConfigurationKeys.LINKER_ARGS, existingLinkerArgs + jitLinkerFlags)

    val linkerOutputKind = determineLinkerOutput(context)
    val linkerPhaseInput = LinkerPhaseInput(
            outputFiles.nativeBinaryFile,
            linkerOutputKind,
            listOf(hostObjectFile.canonicalPath),
            hotReloadOutput.dependenciesTrackingResult,
            outputFiles,
            temporaryFiles,
            ResolvedCacheBinaries(jitCaches, resolvedCaches.dynamic),
    )

    runAndMeasurePhase(LinkerPhase, linkerPhaseInput)
}

private fun isForceLoadCache(path: String): Boolean {
    // At the moment, it gets very hard to load at runtime object files not coming from the K/N compiler
    // (e.g., skiko raises different issues, so it is embedded within the actual host).
    return path.contains("stdlib-cache") || path.contains("skiko")
}

/**
 * Compiles and links a framework for hot-reload.
 *
 * The framework binary contains the full module (runtime + user code + ObjC stubs + class metadata).
 * The user code in the binary is dead weight, live execution goes through JITLink.
 */
internal fun <C : NativeBackendPhaseContext> PhaseEngine<C>.compileAndLinkForHotReloadFramework(
        moduleOutput: ModuleCompilationOutput,
        outputFiles: OutputFiles,
        temporaryFiles: TempFiles,
) {
    /*
    * Additionally, user code is compiled into a separate bootstrap.o (with stdlib as external),
    * loaded via JITLink at startup. The `knhr_stub_*` functions in the framework resolve their
    * IMPs from the JIT'd bootstrap code via `KNHR_LoadObjCStubAddress`.
    *
    * The bootstrap.o and its cache-deps manifest are placed inside the framework bundle
    * so the runtime can locate them via CFBundle at startup.
    */

    val configurables = context.config.platform.configurables

    val bootstrapObjectFile = File(outputFiles.outputName + ".bootstrap.o")
    runAndMeasurePhase(ObjectFilesPhase, ObjectFilesPhaseInput(moduleOutput.bitcodeFile, bootstrapObjectFile))

    // Resolve cache binaries needed by the bootstrap guest code
    val resolvedCaches = resolveCacheBinaries(context.config.cachedLibraries, moduleOutput.dependenciesTrackingResult)
    val (_, jitCaches) = resolvedCaches.static.partition(::isForceLoadCache)

    // Write the cache-deps manifest (list of static library paths for JIT to load at runtime)
    val cacheManifestFile = File(bootstrapObjectFile.path + ".$MANIFEST_DEPS_FILE_EXTENSION")
    cacheManifestFile.writeText(jitCaches.joinToString("\n"))

    val jitLinkerFlags = listOf(
            "-L${configurables.absoluteLlvmHome}/lib",
            "-Wl,-rpath,${configurables.absoluteLlvmHome}/lib",
            "-Wl,-undefined,dynamic_lookup",
            "-Wl,-export_dynamic",
    ) + configurables.llvmJitLibs

    val existingLinkerArgs = context.config.configuration.getList(NativeConfigurationKeys.LINKER_ARGS)
    context.config.configuration.put(NativeConfigurationKeys.LINKER_ARGS, existingLinkerArgs + jitLinkerFlags)

    compileAndLink(moduleOutput, outputFiles.mainFileName, outputFiles, temporaryFiles)

    // Bundle bootstrap.o + manifest into the framework
    // On iOS (flat layout), files go directly in the .framework/ directory.
    // On macOS (versioned layout), files go in Versions/A/Resources/.
    // CFBundleCopyResourceURL handles both layouts transparently at runtime.
    val frameworkDir = File(outputFiles.mainFileName)
    val resourcesDir = when (context.config.target.family) {
        Family.IOS, Family.TVOS, Family.WATCHOS -> frameworkDir
        Family.OSX -> File(frameworkDir, "Versions/A/Resources").also { it.mkdirs() }
        else -> error("Unsupported target family for framework: ${context.config.target.family}")
    }

    bootstrapObjectFile.copyTo(File(resourcesDir, "bootstrap.o"), true)
    val manifestSrc = File("${bootstrapObjectFile.path}.$MANIFEST_DEPS_FILE_EXTENSION")
    if (manifestSrc.exists()) {
        manifestSrc.copyTo(File(resourcesDir, "bootstrap.o.$MANIFEST_DEPS_FILE_EXTENSION"), true)
    }
}

/**
 * Compiles a module for hot-reload guest mode, skipping:
 * 1. Host module creation.
 * 2. Objective-C export class patch.
 * 3. Runtime module collection.
 */
internal fun PhaseEngine<NativeGenerationState>.compileModuleForHotReloadGuest(
        userModule: IrModuleFragment,
        irBuiltIns: IrBuiltIns,
        bootstrapBitcodeFile: File,
        cExportFiles: CExportFiles?,
) {

    // Generate user code into bootstrap module
    // Current context has HotReloadBootstrapLlvmModuleSpecification
    // This makes stdlib types appear as "external", they won't be generated,
    // instead they'll be imported from host at runtime via JITLink
    runBackendCodegenForHotReload(userModule, irBuiltIns, cExportFiles)

    val checkExternalCalls = context.config.checkStateAtExternalCalls
    if (checkExternalCalls) {
        runAndMeasurePhase(CheckExternalCallsPhase)
    }

    // Run bitcode post-processing
    newEngine(context as BitcodePostProcessingContext) { it.runBitcodePostProcessing() }

    if (checkExternalCalls) {
        runAndMeasurePhase(RewriteExternalCallsCheckerGlobals)
    }

    // Write the bootstrap bitcode (user code only)
    LLVMWriteBitcodeToFile(context.llvm.module, bootstrapBitcodeFile.canonicalPath)
}

/**
 * Compiles and links for hot reload GUEST mode.
 */
internal fun <C : NativeBackendPhaseContext> PhaseEngine<C>.compileAndLinkForHotReloadGuest(
        guestOutput: HotReloadGuestCompilationOutput,
        outputFiles: OutputFiles,
) {
    compileBootstrapObjectAndManifest(outputFiles, guestOutput)
}

/**
 * Compile bootstrap (initial user guest code) into an object file and emit the `cache-deps` manifest file.
 * The manifest file contains the paths of platform caches code that need to be loaded at runtime.
 */
private fun <C : NativeBackendPhaseContext> PhaseEngine<C>.compileBootstrapObjectAndManifest(
        outputFiles: OutputFiles,
        guestOutput: HotReloadCompilationOutput
): BootstrapCompilationMetadata {

    // This is the final bootstrap.o that will be loaded by JITLink
    // The bootstrap is not a temporary file (at least for now), since it needs to be loaded by the runtime to start the actual program
    val bootstrapObjectFile = File("${outputFiles.outputName}.bootstrap.o")
    runAndMeasurePhase(ObjectFilesPhase, ObjectFilesPhaseInput(guestOutput.bootstrapBitcodeFile, bootstrapObjectFile))

    // Resolve cache binaries (stdlib, platform libs, etc.) that the host must link against
    val resolvedCaches = resolveCacheBinaries(context.config.cachedLibraries, guestOutput.dependenciesTrackingResult)
    val (forceLoadCaches, jitCaches) = resolvedCaches.static.partition(::isForceLoadCache)

    val cacheManifestFile = File("${outputFiles.outputName}.bootstrap.o.$MANIFEST_DEPS_FILE_EXTENSION")
    cacheManifestFile.writeText(jitCaches.joinToString("\n"))

    return BootstrapCompilationMetadata(bootstrapObjectFile, forceLoadCaches, jitCaches, resolvedCaches)
}

/**
 * Deduplicates an `.a` archive by extracting members and re-archiving unique ones.
 *
 * If the archive has no duplicates, it returns the original path unchanged.
 */
// TODO: this function is a temporary fix, it needs to be removed. Symbol duplication should not happen.
// TODO: Some cache archives (e.g. skiko) contain duplicate `.o` members which cause
// TODO: "duplicate symbol" errors when used with `-force_load`.
private fun deduplicateArchive(
        archivePath: String,
        dedupDir: File,
        config: NativeSecondStageCompilationConfig,
): String {
    val archiveFile = File(archivePath)
    if (!archiveFile.exists()) return archivePath

    val ar = "${config.platform.configurables.absoluteLlvmHome}/bin/llvm-ar"

    val listProcess = ProcessBuilder(ar, "t", archivePath)
            .redirectErrorStream(true).start()

    val members = listProcess.inputStream.bufferedReader().readLines()
    listProcess.waitFor()

    if (members.size == members.toSet().size) return archivePath

    val name = archiveFile.nameWithoutExtension
    val extractDir = File(dedupDir, name).also { it.mkdirs() }
    val arExtractProcess = ProcessBuilder(ar, "x", archivePath)
            .directory(extractDir)
            .redirectErrorStream(true)
            .start()

    require(arExtractProcess.waitFor() == 0) {
        "ar (extraction) failed with exit code different from zero"
    }

    val dedupPath = File(dedupDir, "${name}-dedup.a")
    val objectFiles = extractDir.listFiles()?.filter { it.extension == "o" }?.map { it.name } ?: return archivePath
    val arCreateCmd = listOf(ar, "rcs", dedupPath.absolutePath) + objectFiles
    val arCreateProcess = ProcessBuilder(arCreateCmd)
            .directory(extractDir)
            .redirectErrorStream(true)
            .start()

    require(arCreateProcess.waitFor() == 0) {
        "ar (creation) failed with exit code different from zero"
    }

    return dedupPath.absolutePath
}

