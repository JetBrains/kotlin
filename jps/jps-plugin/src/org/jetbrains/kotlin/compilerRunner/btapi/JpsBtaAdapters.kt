/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerEnumWhenTracker
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerExpectActualTracker
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerFileMappingTracker
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerImportTracker
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerIncrementalCache
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerIncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerInlineConstTracker
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerPackagePartData
import org.jetbrains.kotlin.buildtools.api.jvm.CompilerTargetId
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.OutputItemsCollector
import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.ImportTracker
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import java.io.File

/*
 * Adapters between the Build Tools API's mirror types and the JPS implementations that already exist.
 *
 * JPS keeps its own `*Impl` trackers and its own `JpsIncrementalJvmCache`: everything downstream of the compilation
 * (`updateChunkMappings`, `updateCaches`, `updateLookupStorage`, ...) reads them back and downcasts to the concrete
 * types. These adapters only forward across the API boundary.
 *
 * Every type crossing the boundary here is either a `java.lang.String`, a `ByteArray`, a collection of those, or a
 * `org.jetbrains.kotlin.buildtools.api.*` type, which is exactly what
 * [org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader] shares between the JPS class loader and the
 * isolated implementation class loader.
 */

/**
 * Renders compiler diagnostics into JPS by reporting them onto [collector] and returning a blank string.
 *
 * The Build Tools API implementation drops blank renders instead of logging them, so this hands JPS the structured
 * message (severity + location) instead of a pre-formatted line. Reporting an error here is what eventually sets
 * `Utils.ERRORS_DETECTED_KEY` and turns the build into `ABORT`.
 */
internal class JpsBtaMessageRenderer(private val collector: MessageCollector) : CompilerMessageRenderer {
    override fun render(
        severity: CompilerMessageRenderer.Severity,
        message: String,
        location: CompilerMessageRenderer.SourceLocation?,
    ): String {
        collector.report(severity.toCompilerMessageSeverity(), message, location?.toCompilerMessageSourceLocation())
        return ""
    }

    // `MessageCollectorAdapter.kind` throws on any severity it does not recognise, so this mapping must stay total
    // and must not produce anything outside of ERROR / WARNING / INFO / LOGGING.
    private fun CompilerMessageRenderer.Severity.toCompilerMessageSeverity() = when (this) {
        CompilerMessageRenderer.Severity.ERROR -> CompilerMessageSeverity.ERROR
        CompilerMessageRenderer.Severity.WARNING -> CompilerMessageSeverity.WARNING
        CompilerMessageRenderer.Severity.INFO -> CompilerMessageSeverity.INFO
        CompilerMessageRenderer.Severity.DEBUG -> CompilerMessageSeverity.LOGGING
    }

    private fun CompilerMessageRenderer.SourceLocation.toCompilerMessageSourceLocation() =
        CompilerMessageLocationWithRange.create(path, line, column, lineEnd, columnEnd, lineContent)
}

/**
 * Routes what the Build Tools API implementation itself says onto the JPS message collector.
 *
 * Not everything the API reports goes through [CompilerMessageRenderer]: argument validation errors and restricted
 * argument violations are handed to the [KotlinLogger] instead. Logging those into the IDE log rather than reporting
 * them would leave a build failing with no message attached to it.
 */
internal class JpsBtaMessageCollectorLogger(private val collector: MessageCollector) : KotlinLogger {
    override val isDebugEnabled: Boolean
        get() = true

    override fun error(msg: String, throwable: Throwable?) {
        collector.report(CompilerMessageSeverity.ERROR, msg)
        if (throwable != null) {
            collector.report(CompilerMessageSeverity.LOGGING, throwable.stackTraceToString())
        }
    }

    override fun warn(msg: String, throwable: Throwable?) {
        collector.report(CompilerMessageSeverity.WARNING, msg)
    }

    override fun info(msg: String) {
        collector.report(CompilerMessageSeverity.INFO, msg)
    }

    override fun lifecycle(msg: String) {
        collector.report(CompilerMessageSeverity.INFO, msg)
    }

    override fun debug(msg: String) {
        collector.report(CompilerMessageSeverity.LOGGING, msg)
    }
}

/**
 * Feeds the compiler's source-to-output mapping straight into JPS's [OutputItemsCollector].
 *
 * On the legacy path the same information arrives as `OUTPUT` compiler messages that
 * `JpsCompilerServicesFacadeImpl` parses back out of xml.
 */
internal class JpsBtaFileMappingTracker(private val collector: OutputItemsCollector) : CompilerFileMappingTracker {
    override fun recordSourceFilesToOutputFileMapping(sourceFilePaths: Collection<String>, outputFilePath: String) {
        collector.add(sourceFilePaths.map(::File), File(outputFilePath))
    }

    override fun recordSourceReferencedByCompilerPlugin(sourceFilePath: String) {
        collector.addSourceReferencedByCompilerPlugin(File(sourceFilePath))
    }

    override fun recordOutputFileGeneratedForPlugin(outputFilePath: String) {
        collector.addOutputFileGeneratedForPlugin(File(outputFilePath))
    }

    override fun recordSourceFileGeneratedForPlugin(sourceFilePath: String) {
        collector.addSourceFileGeneratedForPlugin(File(sourceFilePath))
    }
}

/**
 * Forwards lookups into JPS's `LookupTrackerImpl`, which `KotlinBuilder.updateLookupStorage` drains afterwards.
 *
 * The API carries no source position; JPS only ever asks for positions in its own lookup tests, which do not go
 * through [org.jetbrains.kotlin.jps.build.KotlinBuilder].
 */
internal class JpsBtaLookupTracker(private val delegate: LookupTracker) : CompilerLookupTracker {
    override fun recordLookup(
        filePath: String,
        scopeFqName: String,
        scopeKind: CompilerLookupTracker.ScopeKind,
        name: String,
    ) {
        delegate.record(filePath, Position.NO_POSITION, scopeFqName, ScopeKind.valueOf(scopeKind.name), name)
    }

    override fun clear() {
        delegate.clear()
    }
}

internal class JpsBtaExpectActualTracker(private val delegate: ExpectActualTracker) : CompilerExpectActualTracker {
    override fun report(expectedFilePath: String, actualFilePath: String) {
        delegate.report(File(expectedFilePath), File(actualFilePath))
    }

    override fun reportExpectOfLenientStub(expectedFilePath: String) {
        delegate.reportExpectOfLenientStub(File(expectedFilePath))
    }
}

internal class JpsBtaInlineConstTracker(private val delegate: InlineConstTracker) : CompilerInlineConstTracker {
    override fun report(filePath: String, owner: String, name: String, constType: String) {
        delegate.report(filePath, owner, name, constType)
    }
}

internal class JpsBtaEnumWhenTracker(private val delegate: EnumWhenTracker) : CompilerEnumWhenTracker {
    override fun report(whenExpressionFilePath: String, enumClassFqName: String) {
        delegate.report(whenExpressionFilePath, enumClassFqName)
    }
}

internal class JpsBtaImportTracker(private val delegate: ImportTracker) : CompilerImportTracker {
    override fun report(filePath: String, importedFqName: String) {
        delegate.report(filePath, importedFqName)
    }
}

/**
 * Exposes one JPS `JpsIncrementalJvmCache` (through the compiler-facing [IncrementalCache] interface it already
 * implements) to the compiler.
 *
 * [close] is a no-op: JPS owns the storage and closes it at the end of the build.
 */
internal class JpsBtaIncrementalCache(private val delegate: IncrementalCache) : CompilerIncrementalCache {
    override fun getObsoletePackageParts(): Collection<String> = delegate.getObsoletePackageParts()

    override fun getObsoleteMultifileClasses(): Collection<String> = delegate.getObsoleteMultifileClasses()

    override fun getStableMultifileFacadeParts(facadeInternalName: String): Collection<String>? =
        delegate.getStableMultifileFacadeParts(facadeInternalName)

    override fun getPackagePartData(partInternalName: String): CompilerPackagePartData? =
        delegate.getPackagePartData(partInternalName)?.let { CompilerPackagePartData(it.data, it.strings) }

    override fun getModuleMappingData(): ByteArray? = delegate.getModuleMappingData()

    override fun getMetadata(fragmentName: String): Map<String, ByteArray> =
        delegate.getMetadata(fragmentName).mapKeys { it.key.path }

    override fun getClassFilePath(internalClassName: String): String = delegate.getClassFilePath(internalClassName)

    override fun close() {
        // JPS owns the cache lifecycle.
    }
}

/**
 * Hands the compiler the cache of the single target being compiled, whatever [CompilerTargetId] it asks for.
 *
 * Without `-Xbuild-file` the CLI builds exactly one module, named after `-module-name` and unconditionally typed
 * `java-production` (`JvmConfigurationPipelinePhase.configureModuleChunk`). So a test target's `CompilerTargetId`
 * never round-trips, and matching on it would fail. The Build Tools API path only ever handles single-target
 * chunks, so there is exactly one right answer regardless.
 */
internal class JpsBtaSingleTargetIncrementalCompilationComponents(
    private val cache: CompilerIncrementalCache,
) : CompilerIncrementalCompilationComponents {
    override fun getIncrementalCache(target: CompilerTargetId): CompilerIncrementalCache = cache
}
