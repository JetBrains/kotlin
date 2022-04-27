/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION") // Suppress deprecation warning of HasConvention

package org.jetbrains.kotlin.pill

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.*
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.SingleParentCopySpec
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.pill.model.POrderRoot.*
import org.jetbrains.kotlin.pill.model.PSourceRoot.*
import org.jetbrains.kotlin.pill.PillExtensionMirror.*
import org.jetbrains.kotlin.pill.model.*
import java.io.File
import java.util.*
import kotlin.collections.HashMap

typealias OutputDir = String
typealias GradleProjectPath = String

class ModelParser(private val variant: Variant, private val modulePrefix: String) {
    fun parse(project: Project): PProject {
        if (project != project.rootProject) {
            error("$project is not a root project")
        }

        fun Project.matchesSelectedVariant(): Boolean {
            val extension = this.findPillExtensionMirror() ?: return true
            val projectVariant = extension.variant ?: Variant.BASE
            return projectVariant in variant.includes
        }

        val (includedProjects, excludedProjects) = project.allprojects
            .partition { it.plugins.hasPlugin("jps-compatible") && it.matchesSelectedVariant() }

        val modules = includedProjects.flatMap { parseModules(it, excludedProjects) }
        val artifacts = parseArtifacts(project)

        return PProject("Kotlin", project.projectDir, modules, emptyList(), artifacts)
    }

    private fun parseArtifacts(rootProject: Project): Map<String, List<GradleProjectPath>> {
        val artifacts = HashMap<OutputDir, List<GradleProjectPath>>()
        val additionalOutputs = HashMap<OutputDir, List<OutputDir>>()

        for (project in rootProject.allprojects) {
            val sourceSets = project.sourceSets?.toList() ?: emptyList()

            for (sourceSet in sourceSets) {
                val path = makePath(project, sourceSet.name)

                for (output in sourceSet.output.toList()) {
                    artifacts[output.absolutePath] = listOf(path)
                }

                val jarTask = project.tasks.findByName(sourceSet.jarTaskName) as? Jar ?: continue
                val embeddedTask = findEmbeddableTask(project, sourceSet)

                for (task in listOfNotNull(jarTask, embeddedTask)) {
                    val archiveFile = task.archiveFile.get().asFile
                    artifacts[archiveFile.absolutePath] = listOf(path)

                    val additionalOutputsForSourceSet = mutableListOf<File>()
                    fun process(spec: CopySpecInternal) {
                        spec.children.forEach { process(it) }
                        if (spec is SingleParentCopySpec) {
                            for (sourcePath in spec.sourcePaths) {
                                if (sourcePath is SourceSetOutput) {
                                    additionalOutputsForSourceSet += sourcePath.classesDirs
                                    sourcePath.resourcesDir?.let { additionalOutputsForSourceSet += it }
                                }
                            }
                        }
                    }
                    process(task.rootSpec)
                    additionalOutputs[archiveFile.absolutePath] = additionalOutputsForSourceSet.map { it.absolutePath }
                }
            }
        }

        for ((sourceSetOutputDir, additionalOutputsForSourceSet) in additionalOutputs) {
            val projectPaths = artifacts[sourceSetOutputDir] ?: error("Unknown artifact $sourceSetOutputDir")
            val newPaths = projectPaths + additionalOutputsForSourceSet.mapNotNull { artifacts[it] }.flatten()
            artifacts[sourceSetOutputDir] = newPaths.distinct()
        }

        return artifacts
    }

    private fun findEmbeddableTask(project: Project, sourceSet: SourceSet): Jar? {
        val jarName = sourceSet.jarTaskName
        val embeddable = "embeddable"
        val embeddedName = if (jarName == "jar") embeddable else jarName.dropLast("jar".length) + embeddable.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        return project.tasks.findByName(embeddedName) as? Jar
    }

    private fun makePath(project: Project, sourceSetName: String): GradleProjectPath {
        return project.path + "/" + sourceSetName
    }

    private fun parseModules(project: Project, excludedProjects: List<Project>): List<PModule> {
        val modules = mutableListOf<PModule>()

        fun getModuleFile(name: String): File {
            val relativePath = File(project.projectDir, "$modulePrefix$name.iml").toRelativeString(project.rootProject.projectDir)
            return File(project.rootProject.projectDir, ".idea/modules/$relativePath")
        }

        val embeddedDependencies = project.configurations.findByName(EMBEDDED_CONFIGURATION_NAME)
            ?.let { parseDependencies(it) } ?: emptyList()

        val javaLanguageVersion = getJavaLanguageVersion(project)

        val sourceSets = parseSourceSets(project).sortedBy { it.forTests }
        for (sourceSet in sourceSets) {
            val sourceRoots = mutableListOf<PSourceRoot>()

            for (dir in sourceSet.sourceDirectories) {
                sourceRoots += PSourceRoot(dir, if (sourceSet.forTests) Kind.TEST else Kind.PRODUCTION)
            }

            for (dir in sourceSet.resourceDirectories) {
                sourceRoots += PSourceRoot(dir, if (sourceSet.forTests) Kind.TEST_RESOURCES else Kind.RESOURCES)
            }

            if (sourceRoots.isEmpty()) {
                continue
            }

            val productionModule = if (sourceSet.forTests) modules.firstOrNull { !it.forTests } else null

            val contentRoots = sourceRoots.map { PContentRoot(it.directory, listOf(it), emptyList()) }

            var orderRoots = parseDependencies(project, sourceSet)
            if (productionModule != null) {
                val productionModuleDependency = PDependency.Module(productionModule.name)
                orderRoots = listOf(POrderRoot(productionModuleDependency, Scope.COMPILE, true)) + orderRoots
            }

            val name = project.pillModuleName + "." + sourceSet.name

            modules += PModule(
                name = modulePrefix + name,
                path = makePath(project, sourceSet.name),
                forTests = sourceSet.forTests,
                rootDirectory = sourceRoots.first().directory,
                moduleFile = getModuleFile(name),
                contentRoots = contentRoots,
                orderRoots = orderRoots,
                javaLanguageVersion = javaLanguageVersion,
                kotlinOptions = sourceSet.kotlinOptions,
                moduleForProductionSources = productionModule,
                embeddedDependencies = embeddedDependencies
            )
        }

        val mainModuleFileRelativePath = when (project) {
            project.rootProject -> File(project.rootProject.projectDir, modulePrefix + project.rootProject.name + ".iml")
            else -> getModuleFile(project.pillModuleName)
        }

        modules += PModule(
            name = modulePrefix + project.pillModuleName,
            path = project.path,
            forTests = false,
            rootDirectory = project.projectDir,
            moduleFile = mainModuleFileRelativePath,
            contentRoots = listOf(PContentRoot(project.projectDir, listOf(), getExcludedDirs(project, excludedProjects))),
            orderRoots = emptyList(),
            javaLanguageVersion = null,
            kotlinOptions = null,
            moduleForProductionSources = null,
            embeddedDependencies = emptyList()
        )

        return modules
    }

    private fun getJavaLanguageVersion(project: Project): Int? {
        val javaPluginExtension = project.extensions.findByType<JavaPluginExtension>() ?: return null
        val javaToolchainService = project.extensions.findByType<JavaToolchainService>() ?: return null
        return javaToolchainService.launcherFor(javaPluginExtension.toolchain).orNull?.metadata?.languageVersion?.asInt()
    }

    private fun getExcludedDirs(project: Project, excludedProjects: List<Project>): List<File> {
        fun getJavaExcludedDirs() = project.plugins.findPlugin(IdeaPlugin::class.java)
            ?.model?.module?.excludeDirs?.toList() ?: emptyList()

        fun getPillExcludedDirs() = project.findPillExtensionMirror()?.excludedDirs ?: emptyList()

        return getPillExcludedDirs() + getJavaExcludedDirs() + project.buildDir +
                (if (project == project.rootProject) excludedProjects.map { it.buildDir } else emptyList())
    }

    private fun parseSourceSets(project: Project): List<PSourceSet> {
        if (!project.plugins.hasPlugin(JavaPlugin::class.java)) {
            return emptyList()
        }

        val kotlinTasksBySourceSet = project.tasks.names
            .filter { it.startsWith("compile") && it.endsWith("Kotlin") }
            .map { project.tasks.getByName(it) }
            .associateBy { (it.invokeInternal("getSourceSetName") as Property<*>).get() as String }

        val gradleSourceSets = project.sourceSets?.toList() ?: emptyList()
        val sourceSets = mutableListOf<PSourceSet>()

        for (sourceSet in gradleSourceSets) {
            val kotlinCompileTask = kotlinTasksBySourceSet[sourceSet.name]

            fun Any.getKotlin(): SourceDirectorySet {
                val kotlinMethod = javaClass.getMethod("getKotlin")
                kotlinMethod.isAccessible = true
                return kotlinMethod(this) as SourceDirectorySet
            }

            val kotlinSourceDirectories = (sourceSet as HasConvention).convention
                .plugins["kotlin"]?.getKotlin()?.srcDirs ?: emptySet()

            val sourceDirectories = (sourceSet.java.sourceDirectories.files + kotlinSourceDirectories).toList()

            val resourceDirectoriesFromSourceSet = sourceSet.resources.sourceDirectories.files
            val resourceDirectoriesFromTask = parseResourceRootsProcessedByProcessResourcesTask(project, sourceSet)

            val resourceDirectories = (resourceDirectoriesFromSourceSet + resourceDirectoriesFromTask)
                .distinct().filter { it !in sourceDirectories }

            sourceSets += PSourceSet(
                name = sourceSet.name,
                forTests = sourceSet.isTestSourceSet,
                sourceDirectories = sourceDirectories,
                resourceDirectories = resourceDirectories,
                kotlinOptions = kotlinCompileTask?.let { getKotlinOptions(it, project) },
                compileClasspathConfigurationName = sourceSet.compileClasspathConfigurationName,
                runtimeClasspathConfigurationName = sourceSet.runtimeClasspathConfigurationName
            )
        }

        return sourceSets
    }

    private fun parseResourceRootsProcessedByProcessResourcesTask(project: Project, sourceSet: SourceSet): List<File> {
        val isMainSourceSet = sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME

        val taskNameBase = "processResources"
        val taskName = if (isMainSourceSet) taskNameBase else sourceSet.name + taskNameBase.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val task = project.tasks.findByName(taskName) as? ProcessResources ?: return emptyList()

        val roots = mutableListOf<File>()
        fun collectRoots(spec: CopySpecInternal) {
            if (spec is SingleParentCopySpec && spec.children.none()) {
                roots += spec.sourcePaths.map { File(project.projectDir, it.toString()) }.filter { it.exists() }
                return
            }

            spec.children.forEach(::collectRoots)
        }
        collectRoots(task.rootSpec)
        return roots
    }

    private fun getKotlinOptions(kotlinCompileTask: Any, project: Project): PSourceRootKotlinOptions {
        val compileArguments = run {
            val method = kotlinCompileTask::class.java.getMethod("getSerializedCompilerArguments")
            method.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            method.invoke(kotlinCompileTask) as List<String>
        }

        val extraArguments = getExtraArguments(compileArguments)

        val pluginClasspath = mutableListOf<String>()
        if (project.plugins.hasPlugin("org.jetbrains.kotlin.plugin.serialization")) {
            pluginClasspath += "\$KOTLIN_BUNDLED\$/lib/kotlinx-serialization-compiler-plugin.jar"
        }

        fun parseBoolean(name: String) = compileArguments.contains("-$name")
        fun parseString(name: String) = compileArguments.dropWhile { it != "-$name" }.drop(1).firstOrNull()

        return PSourceRootKotlinOptions(
            parseBoolean("no-stdlib"),
            parseBoolean("no-reflect"),
            parseString("module-name"),
            parseString("api-version"),
            parseString("language-version"),
            parseString("jvm-target"),
            extraArguments,
            pluginClasspath
        )
    }

    private fun parseDependencies(project: Project, sourceSet: PSourceSet): List<POrderRoot> {
        val roots = mutableListOf<POrderRoot>()

        fun process(name: String, scope: Scope) {
            val configuration = project.configurations.findByName(name) ?: return
            roots += parseDependencies(configuration).map { POrderRoot(it, scope) }
        }

        process(sourceSet.compileClasspathConfigurationName, Scope.PROVIDED)
        process(sourceSet.runtimeClasspathConfigurationName, Scope.RUNTIME)

        if (sourceSet.forTests) {
            process("jpsTest", Scope.TEST)
        }

        return roots
    }

    private fun parseDependencies(configuration: Configuration): List<PDependency> {
        return configuration.resolve().map { file ->
            val library = PLibrary(file.name, listOf(file))
            return@map PDependency.ModuleLibrary(library)
        }
    }

    private companion object {
        private val SKIPPED_STANDALONE_ARGUMENTS =
            listOf("-no-stdlib", "-no-reflect", "-Xfriend-paths=", "-Xbuild-file=", "-Xplugin=")

        private val SKIPPED_ARGUMENTS_WITH_VALUE =
            listOf("-classpath", "-d", "-module-name", "-api-version", "-language-version", "-jvm-target", "-P")

        private fun getExtraArguments(args: List<String>): List<String> {
            val result = mutableListOf<String>()
            var index = 0
            while (index < args.size) {
                val arg = args[index]

                if (SKIPPED_STANDALONE_ARGUMENTS.any { arg.startsWith(it) }) {
                    index += 1
                    continue
                } else if (SKIPPED_ARGUMENTS_WITH_VALUE.any { arg.startsWith(it) }) {
                    index += 2 // Skip also the value
                    continue
                }

                result += arg
                index += 1
            }
            return result
        }
    }
}

private val SourceSet.isTestSourceSet: Boolean
    get() = name == SourceSet.TEST_SOURCE_SET_NAME
            || name.endsWith("Test")
            || name.endsWith("Tests")
            || (extra.has("jpsKind") && extra.get("jpsKind") == SourceSet.TEST_SOURCE_SET_NAME)

private fun Any.invokeInternal(name: String, instance: Any = this): Any? {
    val method = javaClass.methods.single { it.name.startsWith(name) && it.parameterTypes.isEmpty() }
    method.isAccessible = true
    return method.invoke(instance)
}

val Project.pillModuleName: String
    get() = path.removePrefix(":").replace(':', '.')

val Project.sourceSets: SourceSetContainer?
    get() {
        val javaExtension = project.extensions.findByType<JavaPluginExtension>() ?: return null
        return javaExtension.sourceSets
    }
