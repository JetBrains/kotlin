/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.PsiScriptAnnotationsCollector
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.runReadAction
import org.jetbrains.kotlin.scripting.scriptFileName
import org.jetbrains.kotlin.scripting.withCorrectExtension
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.*
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty

/**
 * The legacy PSI-based refinement entry points, called outside any FIR session (e.g. by the Kotlin IntelliJ plugin).
 * The package and the file name keep the functions in the `RefineCompilationConfigurationKt` facade for binary compatibility.
 */

fun KtSourceFile.toSourceCode(): SourceCode = when (this) {
    is KtPsiSourceFile -> {
        val originalFile = psiFile.originalFile
        (originalFile as? KtFile)?.let(::KtFileScriptSource) ?: VirtualFileScriptSource(originalFile.virtualFile)
    }
    is KtVirtualFileSourceFile -> VirtualFileScriptSource(virtualFile)
    is KtIoFileSourceFile -> FileScriptSource(file)
    is KtInMemoryTextSourceFile -> LazyTextScriptSource(name, path) { text.toString() }
    else -> LazyTextScriptSource(name, path) { getContentsAsStream().use { it.reader().readText() } }
}

// left for binary compatibility with Kotlin Notebook plugin
fun refineScriptCompilationConfiguration(
    script: SourceCode,
    definition: ScriptDefinition,
    project: Project,
    providedConfiguration: ScriptCompilationConfiguration? = null,
): ScriptCompilationConfigurationResult {
    return refineScriptCompilationConfiguration(script, definition, project, providedConfiguration, null)
}

fun refineScriptCompilationConfiguration(
    script: SourceCode,
    definition: ScriptDefinition,
    project: Project,
    providedConfiguration: ScriptCompilationConfiguration? = null, // if null - take from definition
    knownVirtualFileSources: MutableMap<String, VirtualFileScriptSource>? = null,
): ScriptCompilationConfigurationResult {
    // TODO: add location information on refinement errors
    val ktFileSource = script.toKtFileSource(definition, project)
    val compilationConfiguration = providedConfiguration ?: definition.compilationConfiguration
    return runReadAction {
        collectScriptAnnotations(ktFileSource.ktFile, compilationConfiguration, definition.contextClassLoader)
    }.onSuccess { collectedData ->
        refineScriptCompilationConfiguration(
            compilationConfiguration,
            script,
            collectedData,
            knownVirtualFileSources,
            definition
        )
    }
}

fun refineScriptCompilationConfiguration(
    compilationConfiguration: ScriptCompilationConfiguration,
    sourceCode: SourceCode,
    collectedData: ScriptCollectedData,
    knownVirtualFileSources: MutableMap<String, VirtualFileScriptSource>?,
    definition: ScriptDefinition,
): ResultWithDiagnostics<ScriptCompilationConfigurationWrapper> =
    compilationConfiguration.refineOnAnnotations(sourceCode, collectedData)
        .onSuccess {
            it.refineBeforeCompiling(sourceCode, collectedData)
        }.onSuccess {
            it.resolveImportsToVirtualFiles(knownVirtualFileSources)
        }.onSuccess {
            ScriptCompilationConfigurationWrapper(
                sourceCode,
                it.adjustByDefinition(definition)
            ).asSuccess()
        }

fun ScriptCompilationConfiguration.adjustByDefinition(definition: ScriptDefinition): ScriptCompilationConfiguration =
    this.withUpdatedClasspath(additionalClasspath(definition))

private fun additionalClasspath(definition: ScriptDefinition): List<File> {
    return definition.hostConfiguration[ScriptingHostConfiguration.configurationDependencies].toClassPathOrEmpty()
}

fun ScriptCompilationConfiguration.resolveImportsToVirtualFiles(
    knownFileBasedSources: MutableMap<String, VirtualFileScriptSource>?,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    // the resolving is needed while CoreVirtualFS does not cache the files, so attempt to find vf and then PSI by path leads
    // to different PSI files, which breaks mappings needed by script descriptor
    // resolving only to virtual file allows to simplify serialization and maybe a bit more future proof

    val localFS: VirtualFileSystem by lazy(LazyThreadSafetyMode.NONE) {
        val fileManager = VirtualFileManager.getInstance()
        fileManager.getFileSystem(StandardFileSystems.FILE_PROTOCOL)
    }

    val resolvedImports = get(ScriptCompilationConfiguration.importScripts)?.map { sourceCode ->
        when (sourceCode) {
            is VirtualFileScriptSource -> sourceCode
            is FileBasedScriptSource -> {
                val path = sourceCode.file.normalize().absolutePath
                knownFileBasedSources?.get(path) ?: run {
                    val virtualFile = localFS.findFileByPath(path)
                        ?: return@resolveImportsToVirtualFiles makeFailureResult("Imported source file not found: ${sourceCode.file}".asErrorDiagnostics())
                    VirtualFileScriptSource(virtualFile).also {
                        knownFileBasedSources?.set(path, it)
                    }
                }
            }

            else -> {
                // TODO: support knownFileBasedSources here as well
                val scriptFileName = sourceCode.scriptFileName(sourceCode, this)
                val virtualFile = ScriptLightVirtualFile(
                    scriptFileName,
                    sourceCode.locationId,
                    sourceCode.text
                )
                VirtualFileScriptSource(virtualFile)
            }
        }
    }

    val updatedConfiguration = if (resolvedImports.isNullOrEmpty()) this else this.with { resolvedImportScripts(resolvedImports) }
    return updatedConfiguration.asSuccess()
}

fun SourceCode.getVirtualFile(definition: ScriptDefinition?): VirtualFile {
    if (this is VirtualFileScriptSource) return virtualFile
    if (this is KtFileScriptSource) {
        return virtualFile
    }
    if (this is FileScriptSource) {
        val vFile = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL)
            ?.findFileByPath(file.absoluteFile.invariantSeparatorsPath)
        if (vFile != null) return vFile
    }
    val scriptName = withCorrectExtension(name ?: definition?.defaultClassName ?: "script", definition?.fileExtension)
    val scriptPath = when (this) {
        is FileScriptSource -> file.path
        is ExternalSourceCode -> externalLocation.toString()
        else -> null
    }
    val scriptText = getMergedScriptText(this, definition?.compilationConfiguration)

    return ScriptLightVirtualFile(scriptName, scriptPath, scriptText)
}

fun SourceCode.getKtFile(definition: ScriptDefinition?, project: Project): KtFile =
    if (this is KtFileScriptSource) ktFile
    else {
        val file = getVirtualFile(definition)
        ApplicationManager.getApplication().runReadAction<KtFile> {
            val psiFile: PsiFile = PsiManager.getInstance(project).findFile(file)
                ?: throw IllegalArgumentException("Unable to load PSI from ${file.path}")
            (psiFile as? KtFile)
                ?: throw IllegalArgumentException("Not a kotlin file ${file.path} (${file.fileType.name})")
        }
    }

fun SourceCode.toKtFileSource(definition: ScriptDefinition, project: Project): KtFileScriptSource =
    this as? KtFileScriptSource ?: KtFileScriptSource(this.getKtFile(definition, project))

private fun collectScriptAnnotations(
    scriptFile: KtFile,
    compilationConfiguration: ScriptCompilationConfiguration,
    contextClassLoader: ClassLoader?,
): ResultWithDiagnostics<ScriptCollectedData> =
    PsiScriptAnnotationsCollector().collectAnnotations(
        scriptFile, compilationConfiguration, compilationConfiguration.hostConfigurationOrDefault(contextClassLoader)
    )

internal fun ScriptCompilationConfiguration.hostConfigurationOrDefault(contextClassLoader: ClassLoader?): ScriptingHostConfiguration =
    this[ScriptCompilationConfiguration.hostConfiguration]
        ?: defaultJvmScriptingHostConfiguration.let { default ->
            if (contextClassLoader == null) default
            else ScriptingHostConfiguration(default) { jvm.baseClassLoader(contextClassLoader) }
        }

// the diagnostics of the annotation collecting are not returned here, but the annotations which cannot be constructed are returned as
// `InvalidScriptResolverAnnotation`s
fun getScriptCollectedData(
    scriptFile: KtFile,
    compilationConfiguration: ScriptCompilationConfiguration,
    contextClassLoader: ClassLoader?,
): ScriptCollectedData =
    collectScriptAnnotations(scriptFile, compilationConfiguration, contextClassLoader).valueOrNull()
        ?: ScriptCollectedData(emptyMap())
