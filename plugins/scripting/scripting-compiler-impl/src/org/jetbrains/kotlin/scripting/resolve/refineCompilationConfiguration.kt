/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.runReadAction
import org.jetbrains.kotlin.scripting.scriptFileName
import org.jetbrains.kotlin.scripting.withCorrectExtension
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.host.*
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.compat.mapToDiagnostics
import kotlin.script.experimental.jvm.impl.toClassPathOrEmpty
import kotlin.script.experimental.jvm.impl.toDependencies
import kotlin.script.experimental.util.PropertiesCollection

internal fun VirtualFile.loadAnnotations(
    acceptedAnnotations: List<KClass<out Annotation>>,
    project: Project,
    classLoader: ClassLoader?
): List<Annotation> =
// TODO_R: report error on failure to load annotation class
    ApplicationManager.getApplication().runReadAction<List<Annotation>> {
        this.getAnnotationEntries(project)
            .construct(classLoader, acceptedAnnotations, project)
            .map { it.first }
    }

internal fun VirtualFile.getAnnotationEntries(project: Project): Iterable<KtAnnotationEntry> {
    val psiFile: PsiFile = PsiManager.getInstance(project).findFile(this)
        ?: throw IllegalArgumentException("Unable to load PSI from $canonicalPath")
    return (psiFile as? KtFile)?.annotationEntries
        ?: throw IllegalArgumentException("Unable to extract kotlin annotations from $name (${fileType.name})")
}

/**
 * The implementation of the SourceCode for a script located in a virtual file
 */
open class VirtualFileScriptSource(val virtualFile: VirtualFile, private val preloadedText: String? = null) :
    FileBasedScriptSource() {
    override val file: File get() = File(virtualFile.path)
    override val externalLocation: URL get() = URL(virtualFile.url)
    override val text: String by lazy { preloadedText ?: virtualFile.inputStream.bufferedReader().readText() }
    override val name: String? get() = virtualFile.name
    override val locationId: String? get() = virtualFile.path

    override fun equals(other: Any?): Boolean =
        this === other || (other as? VirtualFileScriptSource)?.let { virtualFile == it.virtualFile } == true

    override fun hashCode(): Int = virtualFile.hashCode()
}

/**
 * The implementation of the SourceCode for a script located in a KtFile
 */
open class KtFileScriptSource(val ktFile: KtFile, preloadedText: String? = null) :
    VirtualFileScriptSource(ktFile.virtualFile ?: ktFile.originalFile.virtualFile ?: ktFile.viewProvider.virtualFile, preloadedText) {

    override val text: String by lazy { preloadedText ?: ktFile.text }
    override val name: String? get() = ktFile.name

    override fun equals(other: Any?): Boolean =
        this === other || (other as? KtFileScriptSource)?.let { ktFile == it.ktFile } == true

    override fun hashCode(): Int = ktFile.hashCode()
}

class ScriptLightVirtualFile(name: String, private val _path: String?, text: String) :
    LightVirtualFile(
        name,
        KotlinLanguage.INSTANCE,
        StringUtil.convertLineSeparators(text)
    ) {

    init {
        charset = StandardCharsets.UTF_8
    }

    override fun getPath(): String = _path ?: if (parent != null) parent.path + "/" + name else name

    override fun getCanonicalPath() = path
}

abstract class ScriptCompilationConfigurationWrapper(val script: SourceCode) {
    abstract val configuration: ScriptCompilationConfiguration?

    @Deprecated("Use configuration collection instead")
    abstract val legacyDependencies: ScriptDependencies?

    // optimizing most common ops for the IDE
    // TODO: consider dropping after complete migration
    abstract val dependenciesClassPath: List<File>
    abstract val dependenciesSources: List<File>
    abstract val javaHome: File?
    abstract val defaultImports: List<String>
    abstract val importedScripts: List<SourceCode>

    override fun equals(other: Any?): Boolean = script == (other as? ScriptCompilationConfigurationWrapper)?.script

    override fun hashCode(): Int = script.hashCode()

    class FromCompilationConfiguration(
        script: SourceCode,
        override val configuration: ScriptCompilationConfiguration?
    ) : ScriptCompilationConfigurationWrapper(script) {

        // TODO: check whether implemented optimization for frequent calls makes sense here
        override val dependenciesClassPath: List<File> by lazy {
            configuration?.get(ScriptCompilationConfiguration.dependencies).toClassPathOrEmpty()
        }

        // TODO: check whether implemented optimization for frequent calls makes sense here
        override val dependenciesSources: List<File> by lazy {
            configuration?.get(ScriptCompilationConfiguration.ide.dependenciesSources).toClassPathOrEmpty()
        }

        override val javaHome: File?
            get() = configuration?.get(ScriptCompilationConfiguration.jvm.jdkHome)

        override val defaultImports: List<String>
            get() = configuration?.get(ScriptCompilationConfiguration.defaultImports).orEmpty()

        override val importedScripts: List<SourceCode>
            get() = (configuration?.get(ScriptCompilationConfiguration.resolvedImportScripts) ?: configuration?.get(ScriptCompilationConfiguration.importScripts)).orEmpty()

        @Suppress("OverridingDeprecatedMember", "OVERRIDE_DEPRECATION")
        override val legacyDependencies: ScriptDependencies?
            get() = configuration?.toDependencies(dependenciesClassPath)

        override fun equals(other: Any?): Boolean =
            super.equals(other) && other is FromCompilationConfiguration && configuration == other.configuration

        override fun hashCode(): Int = super.hashCode() + 23 * (configuration?.hashCode() ?: 1)

        override fun toString(): String {
            return "FromCompilationConfiguration($configuration)"
        }
    }

    @Suppress("OverridingDeprecatedMember", "DEPRECATION", "OVERRIDE_DEPRECATION")
    class FromLegacy(
        script: SourceCode,
        override val legacyDependencies: ScriptDependencies?,
        val definition: ScriptDefinition?
    ) : ScriptCompilationConfigurationWrapper(script) {

        override val dependenciesClassPath: List<File>
            get() = legacyDependencies?.classpath.orEmpty()

        override val dependenciesSources: List<File>
            get() = legacyDependencies?.sources.orEmpty()

        override val javaHome: File?
            get() = legacyDependencies?.javaHome

        override val defaultImports: List<String>
            get() = legacyDependencies?.imports.orEmpty()

        override val importedScripts: List<SourceCode>
            get() = legacyDependencies?.scripts?.map { FileScriptSource(it) }.orEmpty()

        override val configuration: ScriptCompilationConfiguration?
            get() {
                val legacy = legacyDependencies ?: return null
                return definition?.compilationConfiguration?.let { config ->
                    ScriptCompilationConfiguration(config) {
                        updateClasspath(legacy.classpath)
                        defaultImports.append(legacy.imports)
                        importScripts.append(legacy.scripts.map { FileScriptSource(it) })
                        jvm {
                            jdkHome.putIfNotNull(legacy.javaHome) // TODO: check if it is correct to supply javaHome as jdkHome
                        }
                        if (legacy.sources.isNotEmpty()) {
                            ide {
                                dependenciesSources.append(JvmDependency(legacy.sources))
                            }
                        }
                    }
                }
            }

        override fun equals(other: Any?): Boolean =
            super.equals(other) && other is FromLegacy && legacyDependencies == other.legacyDependencies

        override fun hashCode(): Int = super.hashCode() + 31 * (legacyDependencies?.hashCode() ?: 1)

        override fun toString(): String {
            return "FromLegacy($legacyDependencies)"
        }
    }
}

typealias ScriptCompilationConfigurationResult = ResultWithDiagnostics<ScriptCompilationConfigurationWrapper>

val ScriptCompilationConfigurationKeys.resolvedImportScripts by PropertiesCollection.key<List<SourceCode>>(isTransient = true)

// left for binary compatibility with Kotlin Notebook plugin
fun refineScriptCompilationConfiguration(
    script: SourceCode,
    definition: ScriptDefinition,
    project: Project,
    providedConfiguration: ScriptCompilationConfiguration? = null,
): ScriptCompilationConfigurationResult {
    return refineScriptCompilationConfiguration(script, definition, project, providedConfiguration, null)
}

@Suppress("DEPRECATION")
fun refineScriptCompilationConfiguration(
    script: SourceCode,
    definition: ScriptDefinition,
    project: Project,
    providedConfiguration: ScriptCompilationConfiguration? = null, // if null - take from definition
    knownVirtualFileSources: MutableMap<String, VirtualFileScriptSource>? = null
): ScriptCompilationConfigurationResult {
    // TODO: add location information on refinement errors
    val ktFileSource = script.toKtFileSource(definition, project)
    val legacyDefinition = definition.asLegacyOrNull<KotlinScriptDefinition>()
    if (legacyDefinition == null) {
        val compilationConfiguration = providedConfiguration ?: definition.compilationConfiguration
        val collectedData =
            runReadAction {
                getScriptCollectedData(ktFileSource.ktFile, compilationConfiguration, project, definition.contextClassLoader)
            }
        return compilationConfiguration.refineOnAnnotations(script, collectedData)
            .onSuccess {
                it.refineBeforeCompiling(script, collectedData)
            }.onSuccess {
                it.resolveImportsToVirtualFiles(knownVirtualFileSources)
            }.onSuccess {
                ScriptCompilationConfigurationWrapper.FromCompilationConfiguration(
                    ktFileSource,
                    it.adjustByDefinition(definition)
                ).asSuccess()
            }
    } else {
        val file = script.getVirtualFile(definition)
        val scriptContents =
            makeScriptContents(file, legacyDefinition, project, definition.contextClassLoader)
        val environment = (legacyDefinition as? KotlinScriptDefinitionFromAnnotatedTemplate)?.environment.orEmpty()

        val result: DependenciesResolver.ResolveResult = try {
            val resolver = legacyDefinition.dependencyResolver
            if (resolver is AsyncDependenciesResolver) {
                // since the only known async resolver is gradle, the following logic is taken from AsyncScriptDependenciesLoader
                // runBlocking is using there to avoid loading dependencies asynchronously
                // because it leads to starting more than one gradle daemon in case of resolving dependencies in build.gradle.kts
                // It is more efficient to use one hot daemon consistently than multiple daemon in parallel
                @Suppress("DEPRECATION_ERROR")
                internalScriptingRunSuspend {
                    resolver.resolveAsync(scriptContents, environment)
                }
            } else {
                resolver.resolve(scriptContents, environment)
            }
        } catch (e: Throwable) {
            return makeFailureResult(e.asDiagnostics(severity = ScriptDiagnostic.Severity.FATAL))
        }
        return if (result is DependenciesResolver.ResolveResult.Failure)
            makeFailureResult(
                result.reports.mapToDiagnostics()
            )
        else
            ScriptCompilationConfigurationWrapper.FromLegacy(
                ktFileSource,
                result.dependencies?.adjustByDefinition(definition),
                definition
            ).asSuccess(result.reports.mapToDiagnostics())
    }
}

fun ScriptDependencies.adjustByDefinition(definition: ScriptDefinition): ScriptDependencies {
    val additionalClasspath = additionalClasspath(definition).filterNot { classpath.contains(it) }
    if (additionalClasspath.isEmpty()) return this

    return copy(classpath = classpath + additionalClasspath)
}

fun ScriptCompilationConfiguration.adjustByDefinition(definition: ScriptDefinition): ScriptCompilationConfiguration {
    return this.withUpdatedClasspath(additionalClasspath(definition))
}

private fun additionalClasspath(definition: ScriptDefinition): List<File> {
    return (definition.asLegacyOrNull<KotlinScriptDefinitionFromAnnotatedTemplate>()?.templateClasspath
        ?: definition.hostConfiguration[ScriptingHostConfiguration.configurationDependencies].toClassPathOrEmpty())
}

fun ScriptCompilationConfiguration.resolveImportsToVirtualFiles(
    knownFileBasedSources: MutableMap<String, VirtualFileScriptSource>?
)
: ResultWithDiagnostics<ScriptCompilationConfiguration> {
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

internal fun makeScriptContents(
    file: VirtualFile,
    legacyDefinition: KotlinScriptDefinition,
    project: Project,
    classLoader: ClassLoader?
): ScriptContentLoader.BasicScriptContents =
    ScriptContentLoader.BasicScriptContents(
        file,
        getAnnotations = {
            file.loadAnnotations(legacyDefinition.acceptedAnnotations, project, classLoader)
        })

fun SourceCode.getVirtualFile(definition: ScriptDefinition): VirtualFile {
    if (this is VirtualFileScriptSource) return virtualFile
    if (this is KtFileScriptSource) {
        return virtualFile
    }
    if (this is FileScriptSource) {
        val vFile = LocalFileSystem.getInstance().findFileByIoFile(file)
        if (vFile != null) return vFile
    }
    val scriptName = withCorrectExtension(name ?: definition.defaultClassName, definition.fileExtension)
    val scriptPath = when (this) {
        is FileScriptSource -> file.path
        is ExternalSourceCode -> externalLocation.toString()
        else -> null
    }
    val scriptText = definition.asLegacyOrNull<KotlinScriptDefinition>()?.let { text }
        ?: getMergedScriptText(this, definition.compilationConfiguration)

    return ScriptLightVirtualFile(scriptName, scriptPath, scriptText)
}

fun SourceCode.getKtFile(definition: ScriptDefinition, project: Project): KtFile =
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
    if (this is KtFileScriptSource) this
    else {
        KtFileScriptSource(this.getKtFile(definition, project))
    }

fun getScriptCollectedData(
    scriptFile: KtFile,
    compilationConfiguration: ScriptCompilationConfiguration,
    project: Project,
    contextClassLoader: ClassLoader?
): ScriptCollectedData {
    val hostConfiguration =
        compilationConfiguration[ScriptCompilationConfiguration.hostConfiguration] ?: defaultJvmScriptingHostConfiguration
    val getScriptingClass = hostConfiguration[ScriptingHostConfiguration.getScriptingClass]
    val jvmGetScriptingClass = (getScriptingClass as? GetScriptingClassByClassLoader)
        ?: throw IllegalArgumentException("Expecting class implementing GetScriptingClassByClassLoader in the hostConfiguration[getScriptingClass], got $getScriptingClass")
    val acceptedAnnotations =
        compilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.flatMap {
            it.annotations.mapNotNull { ann ->
                @Suppress("UNCHECKED_CAST")
                jvmGetScriptingClass(ann, contextClassLoader, hostConfiguration) as? KClass<Annotation> // TODO errors
            }
        }.orEmpty()
    val annotations = scriptFile.annotationEntries.construct(
        contextClassLoader,
        acceptedAnnotations,
        project,
        scriptFile.viewProvider.document,
        scriptFile.virtualFilePath
    )
    return ScriptCollectedData(
        mapOf(
            ScriptCollectedData.collectedAnnotations to annotations,
            ScriptCollectedData.foundAnnotations to annotations.map { it.annotation }
        )
    )
}

private fun Iterable<KtAnnotationEntry>.construct(
    classLoader: ClassLoader?, acceptedAnnotations: List<KClass<out Annotation>>, project: Project, document: Document?, filePath: String
): List<ScriptSourceAnnotation<*>> = construct(classLoader, acceptedAnnotations, project).map { (annotation, psiAnn) ->
    ScriptSourceAnnotation(
        annotation = annotation,
        location = document?.let { document ->
            SourceCode.LocationWithId(
                codeLocationId = filePath,
                locationInText = psiAnn.location(document)
            )
        }
    )
}

private fun Iterable<KtAnnotationEntry>.construct(
    classLoader: ClassLoader?, acceptedAnnotations: List<KClass<out Annotation>>, project: Project
): List<Pair<Annotation, KtAnnotationEntry>> =
    mapNotNull { psiAnn ->
        // TODO: consider advanced matching using semantic similar to actual resolving
        acceptedAnnotations.find { ann ->
            psiAnn.typeName.let { it == ann.simpleName || it == ann.qualifiedName }
        }?.let {
            @Suppress("UNCHECKED_CAST")
            constructAnnotation(
                psiAnn,
                (classLoader ?: ClassLoader.getSystemClassLoader()).loadClass(it.qualifiedName).kotlin as KClass<out Annotation>,
                project
            ) to psiAnn
        }
    }

private fun PsiElement.location(document: Document): SourceCode.Location {
    val start = document.offsetToPosition(startOffset)
    val end = if (endOffset > startOffset) document.offsetToPosition(endOffset) else null
    return SourceCode.Location(start, end)
}

private fun Document.offsetToPosition(offset: Int): SourceCode.Position {
    val line = getLineNumber(offset)
    val column = offset - getLineStartOffset(line)
    return SourceCode.Position(line + 1, column + 1, offset)
}
