/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.impl

import java.io.File
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.ScriptReport
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.compat.mapToLegacyScriptReportPosition
import kotlin.script.experimental.jvm.compat.mapToLegacyScriptReportSeverity

fun ScriptCompilationConfiguration.toDependencies(classpath: List<File>): ScriptDependencies {
    val defaultImports = this[ScriptCompilationConfiguration.defaultImports]?.toList() ?: emptyList()

    return ScriptDependencies(
        classpath = classpath,
        sources = this[ScriptCompilationConfiguration.ide.dependenciesSources].toClassPathOrEmpty(),
        imports = defaultImports,
        scripts = this[ScriptCompilationConfiguration.importScripts].toFilesOrEmpty()
    )
}

internal fun ScriptContents.toScriptSource(): SourceCode = when {
    file != null -> FileScriptSource(file!!, text?.toString())
    text != null -> text!!.toString().toScriptSource()
    else -> throw IllegalArgumentException("Unable to convert script contents $this into script source")
}

fun List<ScriptDependency>?.toClassPathOrEmpty() = this?.flatMap { (it as? JvmDependency)?.classpath ?: emptyList() } ?: emptyList()

internal fun List<SourceCode>?.toFilesOrEmpty() = this?.map {
    val externalSource = it as? ExternalSourceCode
    externalSource?.externalLocation?.toFileOrNull()
        ?: throw RuntimeException("Unsupported source in requireSources parameter - only local files are supported now (${externalSource?.externalLocation})")
} ?: emptyList()

fun ScriptCompilationConfiguration.refineWith(
    handler: RefineScriptCompilationConfigurationHandler?,
    processedScriptData: ScriptCollectedData,
    script: SourceCode
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    if (handler == null) this.asSuccess()
    else handler(ScriptConfigurationRefinementContext(script, this, processedScriptData))