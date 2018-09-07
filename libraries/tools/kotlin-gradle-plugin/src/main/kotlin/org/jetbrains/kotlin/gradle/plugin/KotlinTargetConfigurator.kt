/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.nativeplatform.test.tasks.RunTestExecutable
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KonanCompilerDownloadTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.util.*
import java.util.concurrent.Callable

abstract class AbstractKotlinTargetConfigurator<KotlinTargetType : KotlinTarget>(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    protected val createDefaultSourceSets: Boolean,
    protected val createTestCompilation: Boolean
) {
    open fun configureTarget(
        target: KotlinTargetType
    ) {
        configureCompilationDefaults(target)
        configureCompilations(target)
        defineConfigurationsForTarget(target)
        configureArchivesAndComponent(target)
        configureTest(target)
        configureBuild(target)
    }


    abstract fun configureArchivesAndComponent(target: KotlinTargetType)

    protected fun configureCompilations(platformTarget: KotlinTargetType) {
        val project = platformTarget.project
        val main = platformTarget.compilations.create(KotlinCompilation.MAIN_COMPILATION_NAME)

        platformTarget.compilations.all {
            buildOutputCleanupRegistry.registerOutputs(it.output)
            it.compileDependencyFiles = project.configurations.maybeCreate(it.compileDependencyConfigurationName)
            if (it is KotlinCompilationToRunnableFiles) {
                it.runtimeDependencyFiles = project.configurations.maybeCreate(it.runtimeDependencyConfigurationName)
            }
        }

        if (createTestCompilation) {
            platformTarget.compilations.create(KotlinCompilation.TEST_COMPILATION_NAME).apply {
                compileDependencyFiles = project.files(main.output, project.configurations.maybeCreate(compileDependencyConfigurationName))

                if (this is KotlinCompilationToRunnableFiles) {
                    runtimeDependencyFiles =
                            project.files(output, main.output, project.configurations.maybeCreate(runtimeDependencyConfigurationName))
                }
            }
        }
    }

    protected fun configureCompilationDefaults(target: KotlinTargetType) {
        val project = target.project

        target.compilations.all { compilation ->
            defineConfigurationsForCompilation(compilation, target, project.configurations)

            if (createDefaultSourceSets) {
                project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName).also { sourceSet ->
                    compilation.source(sourceSet) // also adds dependencies, requires the configurations for target and source set to exist at this point
                }
            }

            if (compilation is KotlinCompilationWithResources) {
                configureResourceProcessing(compilation, project.files(Callable { compilation.allKotlinSourceSets.map { it.resources } }))
            }

            createLifecycleTask(compilation)
        }
    }

    protected fun configureTest(target: KotlinTarget) {
        val testCompilation = target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME) as? KotlinCompilationToRunnableFiles
            ?: return // Otherwise, there is no runtime classpath

        target.project.tasks.create(lowerCamelCaseName(target.targetName, testTaskNameSuffix), Test::class.java).apply {
            project.afterEvaluate { // use afterEvaluate to override the JavaPlugin defaults for Test tasks
                conventionMapping.map("testClassesDirs") { testCompilation.output.classesDirs }
                conventionMapping.map("classpath") { testCompilation.runtimeDependencyFiles }
                description = "Runs the unit tests."
                group = JavaBasePlugin.VERIFICATION_GROUP
                target.project.tasks.findByName(JavaBasePlugin.CHECK_TASK_NAME)?.dependsOn(this@apply)
            }
        }
    }

    protected fun configureResourceProcessing(
        compilation: KotlinCompilationWithResources,
        resourceSet: FileCollection
    ) {
        val project = compilation.target.project

        compilation.output.setResourcesDir(Callable {
            val classesDirName = "resources/" + compilation.compilationName
            File(project.buildDir, classesDirName)
        })

        val resourcesTask = project.tasks.maybeCreate(compilation.processResourcesTaskName, ProcessResources::class.java)
        resourcesTask.description = "Processes $resourceSet."
        DslObject(resourcesTask).conventionMapping.map("destinationDir") { compilation.output.resourcesDir }
        resourcesTask.from(resourceSet)
    }

    protected fun createLifecycleTask(compilation: KotlinCompilation) {
        val project = compilation.target.project

        (compilation.output.classesDirs as ConfigurableFileCollection).from(project.files().builtBy(compilation.compileAllTaskName))

        project.tasks.create(compilation.compileAllTaskName).apply {
            group = LifecycleBasePlugin.BUILD_GROUP
            description = "Assembles outputs for compilation '${compilation.name}' of target '${compilation.target.name}'"
            dependsOn(
                compilation.output.dirs,
                compilation.compileKotlinTaskName
            )
            if (compilation is KotlinCompilationWithResources) {
                dependsOn(compilation.processResourcesTaskName)
            }
        }
    }

    protected fun defineConfigurationsForTarget(target: KotlinTargetType) {
        val project = target.project

        val configurations = project.configurations

        val defaultConfiguration = configurations.maybeCreate(target.defaultConfigurationName)
        val mainCompilation = target.compilations.maybeCreate(KotlinCompilation.MAIN_COMPILATION_NAME)

        val compileConfiguration = configurations.maybeCreate(mainCompilation.deprecatedCompileConfigurationName)
        val implementationConfiguration = configurations.maybeCreate(mainCompilation.implementationConfigurationName)

        val runtimeOnlyConfiguration = configurations.maybeCreate(mainCompilation.runtimeOnlyConfigurationName)

        val apiElementsConfiguration = configurations.maybeCreate(target.apiElementsConfigurationName).apply {
            description = "API elements for main."
            isVisible = false
            isCanBeResolved = false
            isCanBeConsumed = true
            attributes.attribute<Usage>(USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_API))
            extendsFrom(configurations.maybeCreate(mainCompilation.apiConfigurationName))
            if (mainCompilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
                extendsFrom(runtimeConfiguration)
            }
            usesPlatformOf(target)
        }

        if (mainCompilation is KotlinCompilationToRunnableFiles) {
            val runtimeElementsConfiguration = configurations.maybeCreate(target.runtimeElementsConfigurationName).apply {
                description = "Elements of runtime for main."
                isVisible = false
                isCanBeConsumed = true
                isCanBeResolved = false
                attributes.attribute<Usage>(USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_RUNTIME_JARS))
                val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
                extendsFrom(implementationConfiguration, runtimeOnlyConfiguration, runtimeConfiguration)
                usesPlatformOf(target)
            }
            defaultConfiguration.extendsFrom(runtimeElementsConfiguration)
        } else {
            defaultConfiguration.extendsFrom(apiElementsConfiguration)
        }

        if (createTestCompilation) {
            val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
            val compileTestsConfiguration = configurations.maybeCreate(testCompilation.deprecatedCompileConfigurationName)
            val testImplementationConfiguration = configurations.maybeCreate(testCompilation.implementationConfigurationName)
            val testRuntimeOnlyConfiguration = configurations.maybeCreate(testCompilation.runtimeOnlyConfigurationName)

            compileTestsConfiguration.extendsFrom(compileConfiguration)
            testImplementationConfiguration.extendsFrom(implementationConfiguration)
            testRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration)

            if (mainCompilation is KotlinCompilationToRunnableFiles && testCompilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
                val testRuntimeConfiguration = configurations.maybeCreate(testCompilation.deprecatedRuntimeConfigurationName)
                testRuntimeConfiguration.extendsFrom(runtimeConfiguration)
            }
        }
    }

    protected fun configureBuild(target: KotlinTargetType) {
        val project = target.project

        val buildNeeded = project.tasks.getByName(JavaBasePlugin.BUILD_NEEDED_TASK_NAME)
        val buildDependent = project.tasks.getByName(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME)

        if (createTestCompilation) {
            val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
            if (testCompilation is KotlinCompilationToRunnableFiles) {
                addDependsOnTaskInOtherProjects(buildNeeded, true, testCompilation.deprecatedRuntimeConfigurationName)
                addDependsOnTaskInOtherProjects(buildDependent, false, testCompilation.deprecatedRuntimeConfigurationName)
            }
        }
    }

    private fun addDependsOnTaskInOtherProjects(task: Task, useDependedOn: Boolean, configurationName: String) {
        val project = task.project
        val configuration = project.configurations.getByName(configurationName)
        task.dependsOn(configuration.getTaskDependencyFromProjectDependency(useDependedOn, task.name))
    }

    companion object {
        const val testTaskNameSuffix = "test"

        fun defineConfigurationsForCompilation(
            compilation: KotlinCompilation,
            target: KotlinTarget,
            configurations: ConfigurationContainer
        ) {
            val compileConfiguration = configurations.maybeCreate(compilation.deprecatedCompileConfigurationName).apply {
                isVisible = false
                isCanBeResolved = true // Needed for IDE import
                description = "Dependencies for $compilation (deprecated, use '${compilation.implementationConfigurationName} ' instead)."
            }

            val apiConfiguration = configurations.maybeCreate(compilation.apiConfigurationName).apply {
                extendsFrom(compileConfiguration)
                isVisible = false
                isCanBeConsumed = false
                isCanBeResolved = false
                description = "API dependencies for $compilation."
            }

            val implementationConfiguration = configurations.maybeCreate(compilation.implementationConfigurationName).apply {
                extendsFrom(compileConfiguration, apiConfiguration)
                isVisible = false
                isCanBeConsumed = false
                isCanBeResolved = false
                description = "Implementation only dependencies for $compilation."
            }

            val compileOnlyConfiguration = configurations.maybeCreate(compilation.compileOnlyConfigurationName).apply {
                isVisible = false
                isCanBeResolved = true // Needed for IDE import
                description = "Compile only dependencies for $compilation."
            }

            val compileClasspathConfiguration = configurations.maybeCreate(compilation.compileDependencyConfigurationName).apply {
                extendsFrom(compileOnlyConfiguration, implementationConfiguration)
                usesPlatformOf(target)
                isVisible = false
                isCanBeConsumed = false
                attributes.attribute(USAGE_ATTRIBUTE, compilation.target.project.usageByName(Usage.JAVA_API))
                description = "Compile classpath for $compilation."
            }

            if (compilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = configurations.maybeCreate(compilation.deprecatedRuntimeConfigurationName).apply {
                    extendsFrom(compileConfiguration)
                    isVisible = false
                    isCanBeResolved = true // Needed for IDE import
                    description =
                            "Runtime dependencies for $compilation (deprecated, use '${compilation.runtimeOnlyConfigurationName} ' instead)."
                }

                val runtimeOnlyConfiguration = configurations.maybeCreate(compilation.runtimeOnlyConfigurationName).apply {
                    isVisible = false
                    isCanBeConsumed = false
                    isCanBeResolved = false
                    description = "Runtime only dependencies for $compilation."
                }

                val runtimeClasspathConfiguration = configurations.maybeCreate(compilation.runtimeDependencyConfigurationName).apply {
                    extendsFrom(runtimeOnlyConfiguration, runtimeConfiguration, implementationConfiguration)
                    usesPlatformOf(target)
                    isVisible = false
                    isCanBeConsumed = false
                    isCanBeResolved = true
                    attributes.attribute(USAGE_ATTRIBUTE, compilation.target.project.usageByName(Usage.JAVA_RUNTIME))
                    description = "Runtime classpath of $compilation."
                }
            }
        }


        internal val KotlinCompilation.deprecatedCompileConfigurationName: String
            get() = disambiguateName("compile")

        internal val KotlinCompilationToRunnableFiles.deprecatedRuntimeConfigurationName: String
            get() = disambiguateName("runtime")
    }
}

open class KotlinTargetConfigurator<KotlinCompilationType: KotlinCompilation>(
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    createDefaultSourceSets: Boolean,
    createTestCompilation: Boolean
) : AbstractKotlinTargetConfigurator<KotlinOnlyTarget<KotlinCompilationType>>(
    buildOutputCleanupRegistry,
    createDefaultSourceSets,
    createTestCompilation
) {

    override fun configureArchivesAndComponent(target: KotlinOnlyTarget<KotlinCompilationType>) {
        val project = target.project

        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val jar = project.tasks.create(target.artifactsTaskName, Jar::class.java)
        jar.description = "Assembles a jar archive containing the main classes."
        jar.group = BasePlugin.BUILD_GROUP
        jar.from(mainCompilation.output)

        val apiElementsConfiguration = project.configurations.getByName(target.apiElementsConfigurationName)

        target.disambiguationClassifier?.let { jar.appendix = it.toLowerCase() }

        // Workaround: adding the artifact during configuration seems to interfere with the Java plugin, which results into missing
        // task dependency 'assemble -> jar' if the Java plugin is applied after this steps
        project.afterEvaluate {
            project.artifacts.add(Dependency.ARCHIVES_CONFIGURATION, jar) { jarArtifact ->
                jarArtifact.builtBy(jar)
                jarArtifact.type = ArtifactTypeDefinition.JAR_TYPE

                addJar(apiElementsConfiguration, jarArtifact)

                if (mainCompilation is KotlinCompilationToRunnableFiles) {
                    val runtimeConfiguration = project.configurations.getByName(mainCompilation.deprecatedRuntimeConfigurationName)
                    val runtimeElementsConfiguration = project.configurations.getByName(target.runtimeElementsConfigurationName)
                    addJar(runtimeConfiguration, jarArtifact)
                    addJar(runtimeElementsConfiguration, jarArtifact)
                    // TODO Check Gradle's special split into variants for classes & resources -- do we need that too?
                }
            }
        }
    }

    private fun addJar(configuration: Configuration, jarArtifact: PublishArtifact) {
        val publications = configuration.outgoing

        // Configure an implicit variant
        publications.artifacts.add(jarArtifact)
        publications.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
    }
}


open class KotlinNativeTargetConfigurator(
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry,
    private val kotlinPluginVersion: String
) : AbstractKotlinTargetConfigurator<KotlinNativeTarget>(
    buildOutputCleanupRegistry,
    createDefaultSourceSets = true,
    createTestCompilation = true
) {
    private val hostTargets = listOf(KonanTarget.LINUX_X64, KonanTarget.MACOS_X64, KonanTarget.MINGW_X64)

    private val Collection<*>.isDimensionVisible: Boolean
        get() = size > 1

    private fun Project.createTestTask(compilation: KotlinNativeCompilation, testExecutableLinkTask: KotlinNativeCompile) {
        val compilationSuffix = compilation.name.takeIf { it != KotlinCompilation.TEST_COMPILATION_NAME }.orEmpty()
        val taskName = lowerCamelCaseName(compilation.target.targetName, compilationSuffix, testTaskNameSuffix)
        val testTask = tasks.create(taskName, RunTestExecutable::class.java).apply {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Executes Kotlin/Native unit tests from the '${compilation.name}' compilation " +
                    "for target '${compilation.target.name}'."
            enabled = compilation.target.konanTarget.isCurrentHost

            val testExecutableProperty = testExecutableLinkTask.outputFile
            executable = testExecutableProperty.get().absolutePath
            // TODO: Provide a normal test path!
            outputDir = project.layout.buildDirectory.dir("test-results").get().asFile

            if (project.hasProperty("teamcity.version")) {
                args("--ktest_logger=TEAMCITY")
            }

            onlyIf { testExecutableProperty.get().exists() }
            inputs.file(testExecutableProperty)
            dependsOn(testExecutableLinkTask)
            dependsOnCompilerDownloading()
        }
        tasks.maybeCreate(LifecycleBasePlugin.CHECK_TASK_NAME).apply {
            dependsOn(testTask)
        }
    }

    private fun Project.binaryOutputDirectory(
        buildType: NativeBuildType,
        kind: NativeOutputKind,
        compilation: KotlinNativeCompilation
    ): File {
        val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
        val buildTypeSubDirectory = buildType.name.toLowerCase()
        val kindSubDirectory = kind.outputDirectoryName

        return buildDir.resolve("bin/$targetSubDirectory${compilation.name}/$buildTypeSubDirectory/$kindSubDirectory")
    }

    private fun Project. klibOutputDirectory(
        compilation: KotlinNativeCompilation
    ): File {
        val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
        return buildDir.resolve("classes/kotlin/$targetSubDirectory${compilation.name}")
    }

    private fun KotlinNativeCompile.addCompilerPlugins() {
        SubpluginEnvironment
            .loadSubplugins(project, kotlinPluginVersion)
            .addSubpluginOptions<CommonCompilerArguments>(project, this, compilerPluginOptions)
        compilerPluginClasspath = project.configurations.getByName(PLUGIN_CLASSPATH_CONFIGURATION_NAME)
    }

    private fun KotlinNativeCompile.registerOutputFiles(outputDirectory: File) {
        val konanTarget = compilation.target.konanTarget

        val prefix = outputKind.prefix(konanTarget)
        val suffix = outputKind.suffix(konanTarget)
        val baseName = if (compilation.isMainCompilation) project.name else compilation.name

        outputFile.set(project.provider {
            var filename = "$prefix$baseName$suffix"
            if (outputKind == CompilerOutputKind.FRAMEWORK ||
                outputKind == CompilerOutputKind.STATIC ||
                outputKind == CompilerOutputKind.DYNAMIC ||
                outputKind == CompilerOutputKind.PROGRAM && konanTarget == KonanTarget.WASM32
            ) {
                filename = filename.replace('-', '_')
            }

            outputDirectory.resolve(filename)
        })

        // Register outputs
        if (outputKind == CompilerOutputKind.FRAMEWORK) {
            outputs.dir(outputFile)
        } else {
            outputs.file(outputFile)
        }

        if (outputKind == CompilerOutputKind.STATIC || outputKind == CompilerOutputKind.DYNAMIC) {
            outputs.file(project.provider {
                val apiFileName = "$prefix${baseName}_api.h".replace('-', '_')
                outputDirectory.resolve(apiFileName)
            })
        }
    }

    private fun Project.createBinaryLinkTasks(compilation: KotlinNativeCompilation) = whenEvaluated {
        val konanTarget = compilation.target.konanTarget
        val availableOutputKinds = compilation.outputKinds.filter { it.availableFor(konanTarget) }
        val linkAll = project.tasks.maybeCreate(compilation.linkAllTaskName)

        for (buildType in compilation.buildTypes) {
            for (kind in availableOutputKinds) {
                val compilerOutputKind = kind.compilerOutputKind

                val linkTask = project.tasks.create(
                    compilation.linkTaskName(kind, buildType),
                    KotlinNativeCompile::class.java
                ).apply {
                    this.compilation = compilation
                    outputKind = compilerOutputKind
                    group = BasePlugin.BUILD_GROUP
                    description = "Links ${kind.description} from the '${compilation.name}' " +
                            "compilation for target '${compilation.platformType.name}'."
                    enabled = compilation.target.konanTarget.enabledOnCurrentHost

                    optimized = buildType.optimized
                    debuggable = buildType.debuggable

                    registerOutputFiles(binaryOutputDirectory(buildType, kind, compilation))
                    addCompilerPlugins()

                    dependsOn(compilation.compileKotlinTaskName)
                    dependsOnCompilerDownloading()
                    linkAll.dependsOn(this)
                }

                compilation.binaryTasks[kind to buildType] = linkTask

                if (compilation.isTestCompilation &&
                    buildType == NativeBuildType.DEBUG &&
                    konanTarget in hostTargets
                ) {
                    // TODO: Refactor and move into the corresponding method of AbstractKotlinTargetConfigurator.
                    createTestTask(compilation, linkTask)
                }
            }
        }
    }

    private fun Project.createKlibArtifact(
        compilation: KotlinNativeCompilation,
        artifactFile: File,
        classifier: String?,
        producingTask: Task
    ) {
        if (!compilation.target.konanTarget.enabledOnCurrentHost) {
            return
        }

        val apiElements = configurations.getByName(compilation.target.apiElementsConfigurationName)
        val klibArtifact = DefaultPublishArtifact(
            compilation.name,
            "klib",
            "klib",
            classifier,
            Date(),
            artifactFile,
            producingTask
        )
        project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(klibArtifact)

        with(apiElements.outgoing) {
            artifacts.add(klibArtifact)
            attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, NativeArtifactFormat.KLIB)
        }
    }

    private fun Project.createRegularKlibArtifact(
        compilation: KotlinNativeCompilation,
        compileTask: KotlinNativeCompile
    ) = createKlibArtifact(compilation, compileTask.outputFile.get(), null, compileTask)

    private fun Project.createCInteropKlibArtifact(
        interop: DefaultCInteropSettings,
        interopTask: CInteropProcess
    ) = createKlibArtifact(interop.compilation, interopTask.outputFile, "cinterop-${interop.name}", interopTask)


    private fun Project.createKlibCompilationTask(compilation: KotlinNativeCompilation) {
        val compileTask = tasks.create(
            compilation.compileKotlinTaskName,
            KotlinNativeCompile::class.java
        ).apply {
            this.compilation = compilation
            outputKind = CompilerOutputKind.LIBRARY
            group = BasePlugin.BUILD_GROUP
            description = "Compiles a klibrary from the '${compilation.name}' " +
                    "compilation for target '${compilation.platformType.name}'."
            enabled = compilation.target.konanTarget.enabledOnCurrentHost

            registerOutputFiles(klibOutputDirectory(compilation))
            addCompilerPlugins()
            dependsOnCompilerDownloading()
            compilation.output.tryAddClassesDir {
                project.files(this.outputFile).builtBy(this)
            }
        }

        project.tasks.getByName(compilation.compileAllTaskName).dependsOn(compileTask)

        if (compilation.compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) {
            project.tasks.getByName(compilation.target.artifactsTaskName).apply {
                dependsOn(compileTask)
                dependsOn(compilation.linkAllTaskName)
            }
            project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).apply {
                dependsOn(compileTask)
                dependsOn(compilation.linkAllTaskName)
            }
            createRegularKlibArtifact(compilation, compileTask)
        }
    }

    private fun Project.createCInteropTasks(compilation: KotlinNativeCompilation) {
        compilation.cinterops.all { interop ->
            val interopTask = tasks.create(interop.interopProcessingTaskName, CInteropProcess::class.java).apply {
                settings = interop
                destinationDir = provider { klibOutputDirectory(compilation) }
                group = INTEROP_GROUP
                description = "Generates Kotlin/Native interop library '${interop.name}' " +
                        "for compilation '${compilation.name}'" +
                        "of target '${konanTarget.name}'."
                enabled = compilation.target.konanTarget.enabledOnCurrentHost

                dependsOnCompilerDownloading()
                val interopOutput = project.files(outputFileProvider).builtBy(this)
                with(compilation) {
                    project.dependencies.add(compileDependencyConfigurationName, interopOutput)
                    output.tryAddClassesDir { interopOutput }
                }
            }
            createCInteropKlibArtifact(interop, interopTask)
        }
    }

    override fun configureArchivesAndComponent(target: KotlinNativeTarget): Unit = with(target.project) {
        tasks.create(target.artifactsTaskName)
        target.compilations.all {
            createKlibCompilationTask(it)
            createBinaryLinkTasks(it)
        }

        with(configurations.getByName(target.apiElementsConfigurationName)) {
            outgoing.attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, NativeArtifactFormat.KLIB)
        }
    }

    protected fun configureCInterops(target: KotlinNativeTarget): Unit = with(target.project) {
        target.compilations.all { compilation ->
            createCInteropTasks(compilation)
            compilation.cinterops.all {
                defineConfigurationsForCInterop(compilation, it, target, configurations)
            }
        }

        if (createTestCompilation) {
            val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)
            target.compilations.findByName(TEST_COMPILATION_NAME)?.apply {
                cinterops.all {
                    it.dependencyFiles += mainCompilation.output
                }
            }
        }
    }

    override fun configureTarget(target: KotlinNativeTarget) {
        super.configureTarget(target)
        configureCInterops(target)
    }

    private fun Task.dependsOnCompilerDownloading() =
        dependsOn(KonanCompilerDownloadTask.KONAN_DOWNLOAD_TASK_NAME)

    object NativeArtifactFormat {
        const val KLIB = "org.jetbrains.kotlin.klib"
    }

    companion object {
        const val INTEROP_GROUP = "interop"

        protected fun defineConfigurationsForCInterop(
            compilation: KotlinNativeCompilation,
            cinterop: CInteropSettings,
            target: KotlinTarget,
            configurations: ConfigurationContainer
        ) {
            val compileOnlyConfiguration = configurations.getByName(compilation.compileOnlyConfigurationName)
            val implementationConfiguration = configurations.getByName(compilation.implementationConfigurationName)

            cinterop.dependencyFiles = configurations.maybeCreate(cinterop.dependencyConfigurationName).apply {
                extendsFrom(compileOnlyConfiguration, implementationConfiguration)
                usesPlatformOf(target)
                isVisible = false
                isCanBeConsumed = false
                attributes.attribute(USAGE_ATTRIBUTE, compilation.target.project.usageByName(Usage.JAVA_API))
                description = "Dependencies for cinterop '${cinterop.name}' (compilation '${compilation.name}')."
            }
        }
    }
}

internal fun Project.usageByName(usageName: String): Usage =
    if (isGradleVersionAtLeast(4, 0)) {
        // `project.objects` is an API introduced in Gradle 4.0
        project.objects.named(Usage::class.java, usageName)
    } else {
        val usagesClass = Class.forName("org.gradle.api.internal.attributes.Usages")
        val usagesMethod = usagesClass.getMethod("usage", String::class.java)
        usagesMethod(null, usageName) as Usage
    }

fun Configuration.usesPlatformOf(target: KotlinTarget): Configuration {
    attributes.attribute(KotlinPlatformType.attribute, target.platformType)
    // TODO: Provide an universal way to copy attributes from the target.
    if (target is KotlinNativeTarget) {
        attributes.attribute(KotlinNativeTarget.konanTargetAttribute, target.konanTarget.name)
    }
    return this
}