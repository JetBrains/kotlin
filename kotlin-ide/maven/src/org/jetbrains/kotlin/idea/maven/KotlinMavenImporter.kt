/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.maven

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.util.PathUtil
import org.jdom.Element
import org.jdom.Text
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.idea.maven.importing.MavenImporter
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter
import org.jetbrains.idea.maven.model.MavenPlugin
import org.jetbrains.idea.maven.project.*
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.compilerRunner.ArgumentUtils
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.idea.facet.*
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.framework.detectLibraryKind
import org.jetbrains.kotlin.idea.maven.configuration.KotlinMavenConfigurator
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.platform.IdePlatformKind
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JsIdePlatformKind
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind
import org.jetbrains.kotlin.platform.impl.isCommon
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.util.*

interface MavenProjectImportHandler {
    companion object : ProjectExtensionDescriptor<MavenProjectImportHandler>(
        "org.jetbrains.kotlin.mavenProjectImportHandler",
        MavenProjectImportHandler::class.java
    )

    operator fun invoke(facet: KotlinFacet, mavenProject: MavenProject)
}

class KotlinMavenImporter : MavenImporter(KOTLIN_PLUGIN_GROUP_ID, KOTLIN_PLUGIN_ARTIFACT_ID) {
    companion object {
        const val KOTLIN_PLUGIN_GROUP_ID = "org.jetbrains.kotlin"
        const val KOTLIN_PLUGIN_ARTIFACT_ID = "kotlin-maven-plugin"

        const val KOTLIN_PLUGIN_SOURCE_DIRS_CONFIG = "sourceDirs"
    }

    override fun preProcess(
        module: Module,
        mavenProject: MavenProject,
        changes: MavenProjectChanges,
        modifiableModelsProvider: IdeModifiableModelsProvider
    ) {
    }

    override fun process(
        modifiableModelsProvider: IdeModifiableModelsProvider,
        module: Module,
        rootModel: MavenRootModelAdapter,
        mavenModel: MavenProjectsTree,
        mavenProject: MavenProject,
        changes: MavenProjectChanges,
        mavenProjectToModuleName: MutableMap<MavenProject, String>,
        postTasks: MutableList<MavenProjectsProcessorTask>
    ) {

        if (changes.plugins) {
            contributeSourceDirectories(mavenProject, module, rootModel)
        }
    }

    override fun postProcess(
        module: Module,
        mavenProject: MavenProject,
        changes: MavenProjectChanges,
        modifiableModelsProvider: IdeModifiableModelsProvider
    ) {
        super.postProcess(module, mavenProject, changes, modifiableModelsProvider)

        if (changes.dependencies) {
            scheduleDownloadStdlibSources(mavenProject, module)

            val targetLibraryKind = detectPlatformByExecutions(mavenProject)?.tooling?.libraryKind
            if (targetLibraryKind != null) {
                modifiableModelsProvider.getModifiableRootModel(module).orderEntries().forEachLibrary { library ->
                    if ((library as LibraryEx).kind == null) {
                        val model = modifiableModelsProvider.getModifiableLibraryModel(library) as LibraryEx.ModifiableModelEx
                        detectLibraryKind(model.getFiles(OrderRootType.CLASSES))?.let { model.kind = it }
                    }
                    true
                }
            }
        }

        configureFacet(mavenProject, modifiableModelsProvider, module)
    }

    private fun scheduleDownloadStdlibSources(mavenProject: MavenProject, module: Module) {
        // TODO: here we have to process all kotlin libraries but for now we only handle standard libraries
        val artifacts = mavenProject.dependencyArtifactIndex.data[KOTLIN_PLUGIN_GROUP_ID]?.values?.flatMap { it.filter { it.isResolved } }
            ?: emptyList()

        val librariesWithNoSources = ArrayList<Library>()
        OrderEnumerator.orderEntries(module).forEachLibrary { library ->
            if (library.modifiableModel.getFiles(OrderRootType.SOURCES).isEmpty()) {
                librariesWithNoSources.add(library)
            }
            true
        }
        val libraryNames = librariesWithNoSources.mapTo(HashSet()) { it.name }
        val toBeDownloaded = artifacts.filter { it.libraryName in libraryNames }

        if (toBeDownloaded.isNotEmpty()) {
            MavenProjectsManager.getInstance(module.project)
                .scheduleArtifactsDownloading(listOf(mavenProject), toBeDownloaded, true, false, AsyncPromise())
        }
    }

    private fun configureJSOutputPaths(
        mavenProject: MavenProject,
        modifiableRootModel: ModifiableRootModel,
        facetSettings: KotlinFacetSettings,
        mavenPlugin: MavenPlugin
    ) {
        fun parentPath(path: String): String =
            File(path).absoluteFile.parentFile.absolutePath

        val sharedOutputFile = mavenPlugin.configurationElement?.getChild("outputFile")?.text

        val compilerModuleExtension = modifiableRootModel.getModuleExtension(CompilerModuleExtension::class.java) ?: return
        val buildDirectory = mavenProject.buildDirectory

        val executions = mavenPlugin.executions

        executions.forEach {
            val explicitOutputFile = it.configurationElement?.getChild("outputFile")?.text ?: sharedOutputFile
            if (PomFile.KotlinGoals.Js in it.goals) {
                // see org.jetbrains.kotlin.maven.K2JSCompilerMojo
                val outputFilePath =
                    PathUtil.toSystemDependentName(explicitOutputFile ?: "$buildDirectory/js/${mavenProject.mavenId.artifactId}.js")
                compilerModuleExtension.setCompilerOutputPath(parentPath(outputFilePath))
                facetSettings.productionOutputPath = outputFilePath
            }
            if (PomFile.KotlinGoals.TestJs in it.goals) {
                // see org.jetbrains.kotlin.maven.KotlinTestJSCompilerMojo
                val outputFilePath = PathUtil.toSystemDependentName(
                    explicitOutputFile ?: "$buildDirectory/test-js/${mavenProject.mavenId.artifactId}-tests.js"
                )
                compilerModuleExtension.setCompilerOutputPathForTests(parentPath(outputFilePath))
                facetSettings.testOutputPath = outputFilePath
            }
        }
    }

    private fun getCompilerArgumentsByConfigurationElement(
        mavenProject: MavenProject,
        configuration: Element?,
        platform: TargetPlatform
    ): List<String> {
        val arguments = platform.createArguments()

        arguments.apiVersion =
            configuration?.getChild("apiVersion")?.text ?: mavenProject.properties["kotlin.compiler.apiVersion"]?.toString()
        arguments.languageVersion =
            configuration?.getChild("languageVersion")?.text ?: mavenProject.properties["kotlin.compiler.languageVersion"]?.toString()
        arguments.multiPlatform = configuration?.getChild("multiPlatform")?.text?.trim()?.toBoolean() ?: false
        arguments.suppressWarnings = configuration?.getChild("nowarn")?.text?.trim()?.toBoolean() ?: false

        configuration?.getChild("experimentalCoroutines")?.text?.trim()?.let {
            arguments.coroutinesState = it
        }

        when (arguments) {
            is K2JVMCompilerArguments -> {
                arguments.classpath = configuration?.getChild("classpath")?.text
                arguments.jdkHome = configuration?.getChild("jdkHome")?.text
                arguments.jvmTarget =
                    configuration?.getChild("jvmTarget")?.text ?: mavenProject.properties["kotlin.compiler.jvmTarget"]?.toString()
                arguments.javaParameters = configuration?.getChild("javaParameters")?.text?.toBoolean() ?: false
            }
            is K2JSCompilerArguments -> {
                arguments.sourceMap = configuration?.getChild("sourceMap")?.text?.trim()?.toBoolean() ?: false
                arguments.sourceMapPrefix = configuration?.getChild("sourceMapPrefix")?.text?.trim() ?: ""
                arguments.sourceMapEmbedSources = configuration?.getChild("sourceMapEmbedSources")?.text?.trim() ?: "inlining"
                arguments.outputFile = configuration?.getChild("outputFile")?.text
                arguments.metaInfo = configuration?.getChild("metaInfo")?.text?.trim()?.toBoolean() ?: false
                arguments.moduleKind = configuration?.getChild("moduleKind")?.text
                arguments.main = configuration?.getChild("main")?.text
            }
        }

        val additionalArgs = SmartList<String>().apply {
            val argsElement = configuration?.getChild("args")

            argsElement?.content?.forEach { argElement ->
                when (argElement) {
                    is Text -> {
                        argElement.text?.let { addAll(splitArgumentString(it)) }
                    }
                    is Element -> {
                        if (argElement.name == "arg") {
                            addIfNotNull(argElement.text)
                        }
                    }
                }
            }
        }
        parseCommandLineArguments(additionalArgs, arguments)

        return ArgumentUtils.convertArgumentsToStringList(arguments)
    }

    private val compilationGoals = listOf(
        PomFile.KotlinGoals.Compile,
        PomFile.KotlinGoals.TestCompile,
        PomFile.KotlinGoals.Js,
        PomFile.KotlinGoals.TestJs,
        PomFile.KotlinGoals.MetaData
    )

    private fun configureFacet(mavenProject: MavenProject, modifiableModelsProvider: IdeModifiableModelsProvider, module: Module) {
        val mavenPlugin = mavenProject.findPlugin(KotlinMavenConfigurator.GROUP_ID, KotlinMavenConfigurator.MAVEN_PLUGIN_ID) ?: return
        val compilerVersion = mavenPlugin.version ?: LanguageVersion.LATEST_STABLE.versionString
        val kotlinFacet = module.getOrCreateFacet(
            modifiableModelsProvider,
            false,
            ExternalProjectSystemRegistry.MAVEN_EXTERNAL_SOURCE_ID
        )

        // TODO There should be a way to figure out the correct platform version
        val platform = detectPlatform(mavenProject)?.defaultPlatform

        kotlinFacet.configureFacet(
            compilerVersion,
            LanguageFeature.Coroutines.defaultState,
            platform,
            modifiableModelsProvider
        )
        val facetSettings = kotlinFacet.configuration.settings
        val configuredPlatform = kotlinFacet.configuration.settings.targetPlatform!!
        val configuration = mavenPlugin.configurationElement
        val sharedArguments = getCompilerArgumentsByConfigurationElement(mavenProject, configuration, configuredPlatform)
        val executionArguments = mavenPlugin.executions
            ?.firstOrNull { it.goals.any { s -> s in compilationGoals } }
            ?.configurationElement?.let { getCompilerArgumentsByConfigurationElement(mavenProject, it, configuredPlatform) }
        parseCompilerArgumentsToFacet(sharedArguments, emptyList(), kotlinFacet, modifiableModelsProvider)
        if (executionArguments != null) {
            parseCompilerArgumentsToFacet(executionArguments, emptyList(), kotlinFacet, modifiableModelsProvider)
        }
        if (facetSettings.compilerArguments is K2JSCompilerArguments) {
            configureJSOutputPaths(mavenProject, modifiableModelsProvider.getModifiableRootModel(module), facetSettings, mavenPlugin)
        }
        MavenProjectImportHandler.getInstances(module.project).forEach { it(kotlinFacet, mavenProject) }
        setImplementedModuleName(kotlinFacet, mavenProject, module)
        kotlinFacet.noVersionAutoAdvance()
    }

    private fun detectPlatform(mavenProject: MavenProject) =
        detectPlatformByExecutions(mavenProject) ?: detectPlatformByLibraries(mavenProject)

    private fun detectPlatformByExecutions(mavenProject: MavenProject): IdePlatformKind<*>? {
        return mavenProject.findPlugin(KOTLIN_PLUGIN_GROUP_ID, KOTLIN_PLUGIN_ARTIFACT_ID)?.executions?.flatMap { it.goals }
            ?.mapNotNull { goal ->
                when (goal) {
                    PomFile.KotlinGoals.Compile, PomFile.KotlinGoals.TestCompile -> JvmIdePlatformKind
                    PomFile.KotlinGoals.Js, PomFile.KotlinGoals.TestJs -> JsIdePlatformKind
                    PomFile.KotlinGoals.MetaData -> CommonIdePlatformKind
                    else -> null
                }
            }?.distinct()?.singleOrNull()
    }

    private fun detectPlatformByLibraries(mavenProject: MavenProject): IdePlatformKind<*>? {
        for (kind in IdePlatformKind.ALL_KINDS) {
            val mavenLibraryIds = kind.tooling.mavenLibraryIds
            if (mavenLibraryIds.any { mavenProject.findDependencies(KOTLIN_PLUGIN_GROUP_ID, it).isNotEmpty() }) {
                // TODO we could return here the correct version
                return kind
            }
        }

        return null
    }

    // TODO in theory it should work like this but it doesn't as it couldn't unmark source roots that are not roots anymore.
    //     I believe this is something should be done by the underlying maven importer implementation or somewhere else in the IDEA
    //     For now there is a contributeSourceDirectories implementation that deals with the issue
    //        see https://youtrack.jetbrains.com/issue/IDEA-148280

    //    override fun collectSourceRoots(mavenProject: MavenProject, result: PairConsumer<String, JpsModuleSourceRootType<*>>) {
    //        for ((type, dir) in collectSourceDirectories(mavenProject)) {
    //            val jpsType: JpsModuleSourceRootType<*> = when (type) {
    //                SourceType.PROD -> JavaSourceRootType.SOURCE
    //                SourceType.TEST -> JavaSourceRootType.TEST_SOURCE
    //            }
    //
    //            result.consume(dir, jpsType)
    //        }
    //    }

    private fun contributeSourceDirectories(mavenProject: MavenProject, module: Module, rootModel: MavenRootModelAdapter) {
        val directories = collectSourceDirectories(mavenProject)

        val toBeAdded = directories.map { it.second }.toSet()
        val state = module.kotlinImporterComponent

        val isNonJvmModule = mavenProject
            .plugins
            .asSequence()
            .filter { it.isKotlinPlugin() }
            .flatMap { it.executions.asSequence() }
            .flatMap { it.goals.asSequence() }
            .any { it in PomFile.KotlinGoals.CompileGoals && it !in PomFile.KotlinGoals.JvmGoals }

        val prodSourceRootType: JpsModuleSourceRootType<*> = if (isNonJvmModule) SourceKotlinRootType else JavaSourceRootType.SOURCE
        val testSourceRootType: JpsModuleSourceRootType<*> =
            if (isNonJvmModule) TestSourceKotlinRootType else JavaSourceRootType.TEST_SOURCE

        for ((type, dir) in directories) {
            if (rootModel.getSourceFolder(File(dir)) == null) {
                val jpsType: JpsModuleSourceRootType<*> = when (type) {
                    SourceType.TEST -> testSourceRootType
                    SourceType.PROD -> prodSourceRootType
                }

                rootModel.addSourceFolder(dir, jpsType)
            }
        }

        if (isNonJvmModule) {
            mavenProject.sources.forEach { rootModel.addSourceFolder(it, SourceKotlinRootType) }
            mavenProject.testSources.forEach { rootModel.addSourceFolder(it, TestSourceKotlinRootType) }
            mavenProject.resources.forEach { rootModel.addSourceFolder(it.directory, ResourceKotlinRootType) }
            mavenProject.testResources.forEach { rootModel.addSourceFolder(it.directory, TestResourceKotlinRootType) }
            KotlinSdkType.setUpIfNeeded()
        }

        state.addedSources.filter { it !in toBeAdded }.forEach {
            rootModel.unregisterAll(it, true, true)
            state.addedSources.remove(it)
        }
        state.addedSources.addAll(toBeAdded)
    }

    private fun collectSourceDirectories(mavenProject: MavenProject): List<Pair<SourceType, String>> =
        mavenProject.plugins.filter { it.isKotlinPlugin() }.flatMap { plugin ->
            plugin.configurationElement.sourceDirectories().map { SourceType.PROD to it } +
                    plugin.executions.flatMap { execution ->
                        execution.configurationElement.sourceDirectories().map { execution.sourceType() to it }
                    }
        }.distinct()

    private fun setImplementedModuleName(kotlinFacet: KotlinFacet, mavenProject: MavenProject, module: Module) {
        if (kotlinFacet.configuration.settings.targetPlatform.isCommon()) {
            kotlinFacet.configuration.settings.implementedModuleNames = emptyList()
        } else {
            val manager = MavenProjectsManager.getInstance(module.project)
            val mavenDependencies = mavenProject.dependencies.mapNotNull { manager?.findProject(it) }
            val implemented = mavenDependencies.filter { detectPlatformByExecutions(it).isCommon }

            kotlinFacet.configuration.settings.implementedModuleNames = implemented.map { manager.findModule(it)?.name ?: it.displayName }
        }
    }
}

private fun MavenPlugin.isKotlinPlugin() =
    groupId == KotlinMavenImporter.KOTLIN_PLUGIN_GROUP_ID && artifactId == KotlinMavenImporter.KOTLIN_PLUGIN_ARTIFACT_ID

private fun Element?.sourceDirectories(): List<String> =
    this?.getChildren(KotlinMavenImporter.KOTLIN_PLUGIN_SOURCE_DIRS_CONFIG)?.flatMap { it.children ?: emptyList() }?.map { it.textTrim }
        ?: emptyList()

private fun MavenPlugin.Execution.sourceType() =
    goals.map { if (isTestGoalName(it)) SourceType.TEST else SourceType.PROD }
        .distinct()
        .singleOrNull() ?: SourceType.PROD

private fun isTestGoalName(goalName: String) = goalName.startsWith("test-")

private enum class SourceType {
    PROD, TEST
}

@State(
    name = "AutoImportedSourceRoots",
    storages = [(Storage(file = StoragePathMacros.MODULE_FILE))]
)
class KotlinImporterComponent : PersistentStateComponent<KotlinImporterComponent.State> {
    class State(var directories: List<String> = ArrayList())

    val addedSources: MutableSet<String> = Collections.synchronizedSet(HashSet<String>())

    override fun loadState(state: State) {
        addedSources.clear()
        addedSources.addAll(state.directories)
    }

    override fun getState(): State {
        return State(addedSources.sorted())
    }
}

private val Module.kotlinImporterComponent: KotlinImporterComponent
    get() = getComponent(KotlinImporterComponent::class.java) ?: throw IllegalStateException("No maven importer state configured")
