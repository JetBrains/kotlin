/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner.btapi

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCache
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerPackagePartData
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerTargetId
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerEnumWhenTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerExpectActualTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerFileMappingTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerImportTracker
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.CompilerInlineConstTracker
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
import java.nio.file.Path

/*
 * This file contains adapters between the Build Tools API types and the JPS implementations that already exist.
 */

/**
 * Reports compiler diagnostics to [collector] with their location instead of rendering them.
 * A blank render is not logged by the Build Tools API implementation.
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

    // `MessageCollectorAdapter` supports only `ERROR`, `WARNING`, `INFO` and `DEBUG` severities and throws on unrecognized ones
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
 * Reports messages of the Build Tools API implementation itself (e.g. argument validation errors) to [collector],
 * otherwise a build could fail without any message.
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
 * Provides source-to-output mapping
 */
internal class JpsBtaFileMappingTracker(private val collector: OutputItemsCollector) : CompilerFileMappingTracker {
    override fun recordSourceFilesToOutputFileMapping(sourceFilePaths: Collection<Path>, outputFilePath: Path) {
        collector.add(sourceFilePaths.map { it.toFile() }, outputFilePath.toFile())
    }

    override fun recordSourceReferencedByCompilerPlugin(sourceFilePath: Path) {
        collector.addSourceReferencedByCompilerPlugin(sourceFilePath.toFile())
    }

    override fun recordOutputFileGeneratedForPlugin(outputFilePath: Path) {
        collector.addOutputFileGeneratedForPlugin(outputFilePath.toFile())
    }

    override fun recordSourceFileGeneratedForPlugin(sourceFilePath: Path) {
        collector.addSourceFileGeneratedForPlugin(sourceFilePath.toFile())
    }
}

/**
 * Note: The Build Tools API carries no lookup positions, JPS needs them only in lookup tests.
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
    override fun report(expectFilePath: Path, actualFilePath: Path) {
        delegate.report(expectFilePath.toFile(), actualFilePath.toFile())
    }

    override fun reportExpectOfLenientStub(expectFilePath: Path) {
        delegate.reportExpectOfLenientStub(expectFilePath.toFile())
    }
}

internal class JpsBtaInlineConstTracker(private val delegate: InlineConstTracker) : CompilerInlineConstTracker {
    override fun report(filePath: Path, owner: String, name: String, constType: String) {
        delegate.report(filePath.toString(), owner, name, constType)
    }
}

internal class JpsBtaEnumWhenTracker(private val delegate: EnumWhenTracker) : CompilerEnumWhenTracker {
    override fun report(whenExpressionFilePath: Path, enumClassFqName: String) {
        delegate.report(whenExpressionFilePath.toString(), enumClassFqName)
    }
}

internal class JpsBtaImportTracker(private val delegate: ImportTracker) : CompilerImportTracker {
    override fun report(filePath: Path, importedFqName: String) {
        delegate.report(filePath.toString(), importedFqName)
    }
}

internal class JpsBtaIncrementalCache(private val delegate: IncrementalCache) : CompilerIncrementalCache {
    override fun getObsoletePackageParts(): Collection<String> = delegate.getObsoletePackageParts()

    override fun getModuleMappingData(): ByteArray? = delegate.getModuleMappingData()

    override fun getMetadata(fragmentName: String): Map<String, ByteArray> =
        delegate.getMetadata(fragmentName).mapKeys { it.key.path }
}

/**
 * Returns the cache of the compiled target for any [CompilerTargetId]
 */
internal class JpsBtaSingleTargetIncrementalCompilationComponents(
    private val cache: CompilerIncrementalCache,
) : CompilerIncrementalCompilationComponents {
    override fun getIncrementalCache(target: CompilerTargetId): CompilerIncrementalCache = cache
}
