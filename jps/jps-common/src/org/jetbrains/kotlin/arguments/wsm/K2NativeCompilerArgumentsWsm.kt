/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*

@Serializable
class K2NativeCompilerArguments : CommonCompilerArguments() {
    // First go the options interesting to the general public.
    // Prepend them with a single dash.
    // Keep the list lexically sorted.

    var enableAssertions: Boolean = false

    var debug: Boolean = false

    var generateTestRunner = false

    var generateWorkerTestRunner = false

    var generateNoExitTestRunner = false

    var includeBinaries: Array<String>? = null

    var libraries: Array<String>? = null

    var libraryVersion: String? = null

    var listTargets: Boolean = false

    var manifestFile: String? = null

    var memoryModel: String? = null

    var moduleName: String? = null

    var nativeLibraries: Array<String>? = null

    var nodefaultlibs: Boolean = false

    var noendorsedlibs: Boolean = false

    var nomain: Boolean = false

    var nopack: Boolean = false

    var linkerArguments: Array<String>? = null

    var singleLinkerArguments: Array<String>? = null

    var nostdlib: Boolean = false

    var optimization: Boolean = false

    var outputName: String? = null

    var mainPackage: String? = null

    var produce: String? = null

    // TODO: remove after 2.0, KT-61098
    var repositories: Array<String>? = null

    var target: String? = null

    // The rest of the options are only interesting to the developers.
    // Make sure to prepend them with -X.
    // Keep the list lexically sorted.

    var bundleId: String? = null

    var cacheDirectories: Array<String>? = null

    var cachedLibraries: Array<String>? = null

    var autoCacheableFrom: Array<String>? = null

    var autoCacheDir: String? = null

    var incrementalCacheDir: String? = null

    var checkDependencies: Boolean = false

    var embedBitcode: Boolean = false

    var embedBitcodeMarker: Boolean = false

    var emitLazyObjCHeader: String? = null

    var exportedLibraries: Array<String>? = null

    var externalDependencies: String? = null

    var fakeOverrideValidator: Boolean = false

    var frameworkImportHeaders: Array<String>? = null

    var lightDebugString: String? = null

    // TODO: remove after 1.4 release.
    var lightDebugDeprecated: Boolean = false

    var generateDebugTrampolineString: String? = null


    var libraryToAddToCache: String? = null

    var filesToCache: Array<String>? = null

    var makePerFileCache: Boolean = false

    var backendThreads: String = "1"

    var exportKDoc: Boolean = false

    var printBitCode: Boolean = false

    var checkExternalCalls: Boolean = false

    var printIr: Boolean = false

    var printFiles: Boolean = false

    var purgeUserLibs: Boolean = false

    var runtimeFile: String? = null

    var includes: Array<String>? = null

    var shortModuleName: String? = null

    var staticFramework: Boolean = false

    var temporaryFilesDir: String? = null

    var saveLlvmIrAfter: Array<String> = emptyArray()

    var verifyBitCode: Boolean = false

    var verifyIr: String? = null

    var verifyCompiler: String? = null

    var friendModules: String? = null

    /**
     * @see K2MetadataCompilerArguments.refinesPaths
     */
    var refinesPaths: Array<String>? = null

    var debugInfoFormatVersion: String = "1" /* command line parser doesn't accept kotlin.Int type */

    var noObjcGenerics: Boolean = false

    var clangOptions: Array<String>? = null

    var allocator: String? = null

    var headerKlibPath: String? = null

    var debugPrefixMap: Array<String>? = null

    var preLinkCaches: String? = null

    // We use `;` as delimiter because properties may contain comma-separated values.
    // For example, target cpu features.
    var overrideKonanProperties: Array<String>? = null

    var destroyRuntimeMode: String? = null

    var gc: String? = null

    var propertyLazyInitialization: String? = null

    // TODO: Remove when legacy MM is gone.
    var workerExceptionHandling: String? = null

    var llvmVariant: String? = null

    var binaryOptions: Array<String>? = null

    var runtimeLogs: String? = null

    var testDumpOutputPath: String? = null

    var lazyIrForCaches: String? = null

    var partialLinkageMode: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var partialLinkageLogLevel: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    var omitFrameworkBinary: Boolean = false

    var compileFromBitcode: String? = null

    var serializedDependencies: String? = null

    var saveDependenciesPath: String? = null

    var saveLlvmIrDirectory: String? = null

    var konanDataDir: String? = null

    override fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> =
        super.configureAnalysisFlags(collector, languageVersion).also {
            val optInList = it[AnalysisFlags.optIn] as List<*>
            it[AnalysisFlags.optIn] = optInList + listOf("kotlin.ExperimentalUnsignedTypes")
            if (printIr)
                phasesToDumpAfter = arrayOf("ALL")
            if (metadataKlib) {
                it[AnalysisFlags.metadataCompilation] = true
            }
        }

    override fun checkIrSupport(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
        if (languageVersionSettings.languageVersion < LanguageVersion.KOTLIN_1_4
            || languageVersionSettings.apiVersion < ApiVersion.KOTLIN_1_4
        ) {
            collector.report(
                severity = CompilerMessageSeverity.ERROR,
                message = "Native backend cannot be used with language or API version below 1.4"
            )
        }
    }

    override fun copyOf(): Freezable = copyK2NativeCompilerArguments(this, K2NativeCompilerArguments())

    companion object {
        const val EMBED_BITCODE_FLAG = "-Xembed-bitcode"
        const val EMBED_BITCODE_MARKER_FLAG = "-Xembed-bitcode-marker"
        const val STATIC_FRAMEWORK_FLAG = "-Xstatic-framework"
        const val INCLUDE_ARG = "-Xinclude"
        const val CACHED_LIBRARY = "-Xcached-library"
        const val ADD_CACHE = "-Xadd-cache"
        const val INCREMENTAL_CACHE_DIR = "-Xic-cache-dir"
        const val SHORT_MODULE_NAME_ARG = "-Xshort-module-name"
    }
}