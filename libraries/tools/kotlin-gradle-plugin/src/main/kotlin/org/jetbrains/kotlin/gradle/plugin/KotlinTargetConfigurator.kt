/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
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
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.tasks.KonanCompilerDownloadTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.util.*
import java.util.concurrent.Callable

abstract class AbstractKotlinTargetConfigurator<KotlinTargetType : KotlinTarget>(
    private val buildOutputCleanupRegistry: BuildOutputCleanupRegistry
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

        platformTarget.compilations.create(KotlinCompilation.TEST_COMPILATION_NAME).apply {
            compileDependencyFiles = project.files(main.output, project.configurations.maybeCreate(compileDependencyConfigurationName))

            if (this is KotlinCompilationToRunnableFiles) {
                runtimeDependencyFiles = project.files(output, main.output, project.configurations.maybeCreate(runtimeDependencyConfigurationName))
            }
        }

    }

    protected fun configureCompilationDefaults(target: KotlinTargetType) {
        val project = target.project

        target.compilations.all { compilation ->
            defineConfigurationsForCompilation(compilation, target, project.configurations)

            project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName).also { sourceSet ->
                compilation.source(sourceSet) // also adds dependencies, requires the configurations for target and source set to exist at this point
            }

            if (compilation is KotlinCompilationWithResources) {
                val sourceSetHierarchy = compilation.kotlinSourceSets.flatMap { it.getSourceSetHierarchy() }.distinct()
                configureResourceProcessing(compilation, project.files(Callable { sourceSetHierarchy.map { it.resources } }))
            }

            createLifecycleTask(compilation)
        }
    }

    protected fun configureTest(target: KotlinTarget) {
        val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME) as? KotlinCompilationToRunnableFiles
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
            description = "Assembles " + compilation.output + ""
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
        val testCompilation = target.compilations.maybeCreate(KotlinCompilation.TEST_COMPILATION_NAME)

        val compileConfiguration = configurations.maybeCreate(mainCompilation.deprecatedCompileConfigurationName)
        val implementationConfiguration = configurations.maybeCreate(mainCompilation.implementationConfigurationName)

        val runtimeOnlyConfiguration = configurations.maybeCreate(mainCompilation.runtimeOnlyConfigurationName)
        val compileTestsConfiguration = configurations.maybeCreate(testCompilation.deprecatedCompileConfigurationName)
        val testImplementationConfiguration = configurations.maybeCreate(testCompilation.implementationConfigurationName)
        val testRuntimeOnlyConfiguration = configurations.maybeCreate(testCompilation.runtimeOnlyConfigurationName)

        compileTestsConfiguration.extendsFrom(compileConfiguration)
        testImplementationConfiguration.extendsFrom(implementationConfiguration)
        testRuntimeOnlyConfiguration.extendsFrom(runtimeOnlyConfiguration)

        configurations.maybeCreate(target.apiElementsConfigurationName).apply {
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

        val runtimeElementsConfiguration = configurations.maybeCreate(target.runtimeElementsConfigurationName).apply {
            description = "Elements of runtime for main."
            isVisible = false
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes.attribute<Usage>(USAGE_ATTRIBUTE, project.usageByName(Usage.JAVA_RUNTIME_JARS))
            if (mainCompilation is KotlinCompilationToRunnableFiles) {
                val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
                extendsFrom(implementationConfiguration, runtimeOnlyConfiguration, runtimeConfiguration)
            }
            usesPlatformOf(target)
        }

        if (mainCompilation is KotlinCompilationToRunnableFiles && testCompilation is KotlinCompilationToRunnableFiles) {
            val runtimeConfiguration = configurations.maybeCreate(mainCompilation.deprecatedRuntimeConfigurationName)
            val testRuntimeConfiguration = configurations.maybeCreate(testCompilation.deprecatedRuntimeConfigurationName)
            testRuntimeConfiguration.extendsFrom(runtimeConfiguration)
        }

        defaultConfiguration.extendsFrom(runtimeElementsConfiguration).usesPlatformOf(target)
    }

    protected fun configureBuild(target: KotlinTargetType) {
        val project = target.project

        val testCompilation = target.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
        val buildNeeded = project.tasks.getByName(JavaBasePlugin.BUILD_NEEDED_TASK_NAME)
        val buildDependent = project.tasks.getByName(JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME)

        if (testCompilation is KotlinCompilationToRunnableFiles) {
            addDependsOnTaskInOtherProjects(buildNeeded, true, testCompilation.deprecatedRuntimeConfigurationName)
            addDependsOnTaskInOtherProjects(buildDependent, false, testCompilation.deprecatedRuntimeConfigurationName)
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
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry
) : AbstractKotlinTargetConfigurator<KotlinOnlyTarget<KotlinCompilationType>>(buildOutputCleanupRegistry) {

    override fun configureArchivesAndComponent(target: KotlinOnlyTarget<KotlinCompilationType>) {
        val project = target.project

        val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

        val jar = project.tasks.create(target.artifactsTaskName, Jar::class.java)
        jar.description = "Assembles a jar archive containing the main classes."
        jar.group = BasePlugin.BUILD_GROUP
        jar.from(mainCompilation.output)

        val apiElementsConfiguration = project.configurations.getByName(target.apiElementsConfigurationName)

        target.disambiguationClassifier?.let { jar.classifier = it }

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
    buildOutputCleanupRegistry: BuildOutputCleanupRegistry
) : AbstractKotlinTargetConfigurator<KotlinNativeTarget>(buildOutputCleanupRegistry) {

    private val Collection<*>.isDimensionVisible: Boolean
        get() = size > 1

    private fun createDimensionSuffix(dimensionName: String, multivalueProperty: Collection<*>): String =
        if (multivalueProperty.isDimensionVisible) {
            dimensionName.toLowerCase().capitalize()
        } else {
            ""
        }

    private fun Project.createTestTask(compilation: KotlinNativeCompilation, testExecutableLinkTask: KotlinNativeCompile) {
        val taskName = lowerCamelCaseName("run", compilation.name, compilation.target.name)
        val testTask = tasks.create(taskName, RunTestExecutable::class.java).apply {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Executes Kotlin/Native unit tests from the '${compilation.name}' compilation " +
                    "for target '${compilation.platformType.name}'"

            val testExecutableProperty = testExecutableLinkTask.outputFile
            executable = testExecutableProperty.get().absolutePath
            // TODO: Provide a normal test path!
            outputDir = project.layout.buildDirectory.dir("test-results").get().asFile

            onlyIf { testExecutableProperty.get().exists() }
            inputs.file(testExecutableProperty)
            dependsOn(testExecutableLinkTask)
            dependsOnCompilerDownloading()
        }
        tasks.maybeCreate(LifecycleBasePlugin.CHECK_TASK_NAME).apply {
            dependsOn(testTask)
        }
    }

    private fun Project.createBinaryLinkTasks(compilation: KotlinNativeCompilation) = whenEvaluated {
        val konanTarget = compilation.target.konanTarget
        val buildTypes = compilation.buildTypes
        val availableOutputKinds = compilation.outputKinds.filter { it.availableFor(konanTarget) }
        val linkAll = project.tasks.maybeCreate(compilation.linkAllTaskName)

        for (buildType in compilation.buildTypes) {
            for (kind in availableOutputKinds) {
                val compilerOutputKind = kind.compilerOutputKind

                val compilationSuffix = compilation.name
                val buildTypeSuffix = createDimensionSuffix(buildType.name, buildTypes)
                val targetSuffix = compilation.target.name
                val kindSuffix = kind.taskNameClassifier
                val taskName = lowerCamelCaseName("link", compilationSuffix, buildTypeSuffix, kindSuffix, targetSuffix)

                val linkTask = project.tasks.create(
                    taskName,
                    KotlinNativeCompile::class.java
                ).apply {
                    this.compilation = compilation
                    outputKind = compilerOutputKind
                    group = BasePlugin.BUILD_GROUP
                    description = "Links ${kind.description} from the '${compilation.name}' " +
                            "compilation for target '${compilation.platformType.name}'"

                    optimized = buildType.optimized
                    debuggable = buildType.debuggable

                    if (outputKind == CompilerOutputKind.FRAMEWORK) {
                        outputs.dir(outputFile)
                    } else {
                        outputs.file(outputFile)
                    }

                    outputFile.set(provider {
                        val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
                        val buildTypeSubDirectory = buildType.name.toLowerCase()
                        val compilationName = compilation.compilationName
                        val prefix = compilerOutputKind.prefix(konanTarget)
                        val suffix = compilerOutputKind.suffix(konanTarget)
                        var filename = "$prefix$compilationName$suffix"
                        if (outputKind == CompilerOutputKind.FRAMEWORK) {
                            filename = filename.replace('-', '_')
                        }
                        File(project.buildDir, "bin/$targetSubDirectory$compilationName/$buildTypeSubDirectory/$filename")
                    })

                    dependsOnCompilerDownloading()
                    linkAll.dependsOn(this)
                }

                if (compilation.isTestCompilation &&
                    buildType == NativeBuildType.DEBUG &&
                    konanTarget == HostManager.host
                ) {
                    createTestTask(compilation, linkTask)
                }
            }
        }
    }

    private fun Project.createKlibPublishableArtifact(compilation: KotlinNativeCompilation, compileTask: KotlinNativeCompile) {
        val apiElements = configurations.getByName(compilation.target.apiElementsConfigurationName)
        val klibArtifact = DefaultPublishArtifact(
            compilation.name,
            "klib",
            "klib",
            null,
            Date(),
            compileTask.outputFile.get(),
            compileTask
        )
        compilation.target.disambiguationClassifier?.let { klibArtifact.classifier = it }
        project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(klibArtifact)

        with(apiElements.outgoing) {
            artifacts.add(klibArtifact)
            attributes.attribute(ArtifactAttributes.ARTIFACT_FORMAT, NativeArtifactFormat.KLIB)
        }
    }

    private fun Project.createKlibCompilationTask(compilation: KotlinNativeCompilation) {
        val compileTask = tasks.create(
            compilation.compileKotlinTaskName,
            KotlinNativeCompile::class.java
        ).apply {
            this.compilation = compilation
            outputKind = CompilerOutputKind.LIBRARY
            group = BasePlugin.BUILD_GROUP
            description = "Compiles a klibrary from the '${compilation.name}' " +
                    "compilation for target '${compilation.platformType.name}'"

            outputs.file(outputFile)
            outputFile.set(provider {
                val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
                val classifier = compilation.target.disambiguationClassifier?.let { "-$it" }.orEmpty()
                val compilationName = compilation.compilationName
                val klibName = if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME)
                    project.name
                else
                    compilationName
                File(project.buildDir, "classes/kotlin/$targetSubDirectory$compilationName/$klibName$classifier.klib")
            })

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
            createKlibPublishableArtifact(compilation, compileTask)
        }
    }

    override fun configureArchivesAndComponent(target: KotlinNativeTarget) = with(target.project) {
        if (!HostManager().isEnabled(target.konanTarget)) {
            return
        }

        tasks.create(target.artifactsTaskName)
        target.compilations.all {
            createKlibCompilationTask(it)
            createBinaryLinkTasks(it)
        }
    }

    private fun Task.dependsOnCompilerDownloading() =
        dependsOn(KonanCompilerDownloadTask.KONAN_DOWNLOAD_TASK_NAME)

    object NativeArtifactFormat {
        const val KLIB = "org.jerbrains.kotlin.klib"
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