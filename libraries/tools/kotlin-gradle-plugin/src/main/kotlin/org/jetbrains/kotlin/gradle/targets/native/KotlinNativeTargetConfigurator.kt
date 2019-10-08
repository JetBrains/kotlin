/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.internal.artifacts.ArtifactAttributes
import org.gradle.api.internal.artifacts.publish.DefaultPublishArtifact
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.native.DefaultKotlinNativeTestRun
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeBinaryTestRun
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import java.io.File
import java.util.*

open class KotlinNativeTargetConfigurator<T : KotlinNativeTarget>(
    private val kotlinPluginVersion: String
) : AbstractKotlinTargetConfigurator<T>(
    createDefaultSourceSets = true,
    createTestCompilation = true
) {
    private fun Project.klibOutputDirectory(
        compilation: KotlinNativeCompilation
    ): File {
        val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
        return buildDir.resolve("classes/kotlin/$targetSubDirectory${compilation.name}")
    }

    private fun AbstractKotlinNativeCompile<*>.addCompilerPlugins() {
        SubpluginEnvironment
            .loadSubplugins(project, kotlinPluginVersion)
            .addSubpluginOptions(project, this, compilerPluginOptions)
        compilerPluginClasspath = project.configurations.getByName(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME)
    }

    // region Artifact creation.
    private fun Project.createKlibArtifact(
        compilation: KotlinNativeCompilation,
        artifactFile: File,
        classifier: String?,
        producingTask: Task,
        copy: Boolean = false
    ) {
        if (!compilation.konanTarget.enabledOnCurrentHost) {
            return
        }

        val apiElements = configurations.getByName(compilation.target.apiElementsConfigurationName)

        val realProducingTask: Task
        // TODO: Someone remove this HACK PLEASE!
        val realArtifactFile = if (copy) {
            realProducingTask = project.tasks.create("copy${producingTask.name.capitalize()}", Copy::class.java) {
                val targetSubDirectory = compilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
                it.destinationDir = project.buildDir.resolve("libs/$targetSubDirectory${compilation.name}")
                it.from(artifactFile)
                it.dependsOn(producingTask)
            }
            realProducingTask.destinationDir.resolve(artifactFile.name)
        } else {
            realProducingTask = producingTask
            artifactFile
        }

        val klibArtifact = DefaultPublishArtifact(
            compilation.name,
            "klib",
            "klib",
            classifier,
            Date(),
            realArtifactFile,
            realProducingTask
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
    ) = createKlibArtifact(interop.compilation, interopTask.outputFile, "cinterop-${interop.name}", interopTask, copy = true)
    // endregion.

    // region Task creation.
    private fun Project.createLinkTask(binary: NativeBinary) {
        tasks.create(
            binary.linkTaskName,
            KotlinNativeLink::class.java
        ).apply {
            val target = binary.target
            this.binary = binary
            group = BasePlugin.BUILD_GROUP
            description = "Links ${binary.outputKind.description} '${binary.name}' for a target '${target.name}'."
            enabled = binary.konanTarget.enabledOnCurrentHost
            destinationDir = binary.outputDirectory
            addCompilerPlugins()

            if (binary !is TestExecutable) {
                tasks.maybeCreate(target.artifactsTaskName).dependsOn(this)
                tasks.maybeCreate(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(this)
            }
        }
    }

    private fun Project.createRunTask(binary: Executable) {
        val taskName = binary.runTaskName ?: return
        tasks.create(taskName, Exec::class.java).apply {
            group = RUN_GROUP
            description = "Executes Kotlin/Native executable ${binary.name} for target ${binary.target.name}"

            enabled = binary.konanTarget.isCurrentHost

            executable = binary.outputFile.absolutePath
            workingDir = project.projectDir

            onlyIf { binary.outputFile.exists() }
            dependsOn(binary.linkTaskName)
        }
    }

    private fun Project.createKlibCompilationTask(compilation: KotlinNativeCompilation) {
        val compileTask = tasks.create(
            compilation.compileKotlinTaskName,
            KotlinNativeCompile::class.java
        ).apply {
            this.compilation = compilation
            group = BasePlugin.BUILD_GROUP
            description = "Compiles a klibrary from the '${compilation.name}' " +
                    "compilation for target '${compilation.platformType.name}'."
            enabled = compilation.konanTarget.enabledOnCurrentHost

            destinationDir = klibOutputDirectory(compilation)
            addCompilerPlugins()
            compilation.output.addClassesDir {
                project.files(this.outputFile).builtBy(this)
            }
        }

        project.tasks.getByName(compilation.compileAllTaskName).dependsOn(compileTask)

        if (compilation.compilationName == MAIN_COMPILATION_NAME) {
            project.tasks.getByName(compilation.target.artifactsTaskName).apply {
                dependsOn(compileTask)
            }
            project.tasks.getByName(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).apply {
                dependsOn(compileTask)
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
                enabled = compilation.konanTarget.enabledOnCurrentHost

                val interopOutput = project.files(outputFileProvider).builtBy(this)
                with(compilation) {
                    // Register the interop library as a dependency of the compilation to make IDE happy.
                    project.dependencies.add(compileDependencyConfigurationName, interopOutput)
                    if (isMainCompilation) {
                        // Register the interop library as an outgoing klib to allow depending on projects with cinterops.
                        project.dependencies.add(target.apiElementsConfigurationName, interopOutput)
                        // Add the interop library in publication.
                        createCInteropKlibArtifact(interop, this@apply)
                        // We cannot add the interop library in an compilation output because in this case
                        // IDE doesn't see this library in module dependencies. So we have to manually add
                        // main interop libraries in dependencies of the default test compilation.
                        target.compilations.findByName(TEST_COMPILATION_NAME)?.let { testCompilation ->
                            project.dependencies.add(testCompilation.compileDependencyConfigurationName, interopOutput)
                            testCompilation.cinterops.all {
                                it.dependencyFiles += interopOutput
                            }
                        }
                    }
                }
            }
        }
    }
    // endregion.

    // region Configuration.
    override fun configurePlatformSpecificModel(target: T) {
        configureBinaries(target)
        configureFrameworkExport(target)
        configureCInterops(target)
        warnAboutIncorrectDependencies(target)
    }

    override fun configureArchivesAndComponent(target: T): Unit = with(target.project) {
        tasks.create(target.artifactsTaskName)
        target.compilations.all {
            createKlibCompilationTask(it)
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
                    it.dependencyFiles += mainCompilation.output.allOutputs
                }
            }
        }
    }

    protected fun configureBinaries(target: KotlinNativeTarget) {
        val project = target.project
        // Create link and run tasks.
        target.binaries.all {
            project.createLinkTask(it)
        }

        target.binaries.withType(Executable::class.java).all {
            project.createRunTask(it)
        }

        target.binaries.prefixGroups.all { prefixGroup ->
            val linkGroupTask = project.tasks.maybeCreate(prefixGroup.linkTaskName)
            prefixGroup.binaries.all {
                linkGroupTask.dependsOn(it.linkTaskName)
            }
        }

        // Create an aggregate link task for each compilation.
        target.compilations.all {
            project.tasks.create(it.binariesTaskName)
        }

        project.whenEvaluated {
            target.binaries.forEach {
                project.tasks.getByName(it.compilation.binariesTaskName).dependsOn(it.linkTaskName)
            }
        }

        /**
         * We create test binaries for all platforms but test runs only for host platforms, see [KotlinNativeTargetWithTestsConfigurator]
         */
        target.binaries.test(listOf(NativeBuildType.DEBUG)) { }
    }

    fun configureFrameworkExport(target: KotlinNativeTarget) {
        val project = target.project

        target.compilations.all {
            // Allow resolving api configurations directly to be able to check that
            // all exported dependency are also added in the corresponding api configurations.
            // The check is performed during a link task execution.
            project.configurations.maybeCreate(it.apiConfigurationName).apply {
                isCanBeResolved = true
                usesPlatformOf(target)
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
            }
        }

        target.binaries.withType(Framework::class.java).all { framework ->
            val exportConfiguration = project.configurations.maybeCreate(framework.exportConfigurationName).apply {
                isVisible = false
                isTransitive = false
                isCanBeConsumed = false
                isCanBeResolved = true
                usesPlatformOf(target)
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
                description = "Dependenceis to be exported in framework ${framework.name} for target ${target.targetName}"
            }
        }
    }

    override fun defineConfigurationsForTarget(target: T) {
        super.defineConfigurationsForTarget(target)
        val configurations = target.project.configurations

        // The configuration and the main compilation are created by the base class.
        val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)
        configurations.getByName(target.apiElementsConfigurationName).apply {
            //  K/N compiler doesn't divide libraries into implementation and api ones. So we need to add implementation
            // dependencies into the outgoing configuration.
            extendsFrom(configurations.getByName(mainCompilation.implementationConfigurationName))
        }
    }

    private fun warnAboutIncorrectDependencies(target: KotlinNativeTarget) = target.project.whenEvaluated {

        val compileOnlyDependencies = target.compilations.mapNotNull {
            val dependencies = configurations.getByName(it.compileOnlyConfigurationName).allDependencies
            if (dependencies.isNotEmpty()) {
                it to dependencies
            } else null
        }

        fun Dependency.stringCoordinates(): String = buildString {
            group?.let { append(it).append(':') }
            append(name)
            version?.let { append(':').append(it) }
        }

        if (compileOnlyDependencies.isNotEmpty()) {
            with(target.project.logger) {
                warn("A compileOnly dependency is used in the Kotlin/Native target '${target.name}':")
                compileOnlyDependencies.forEach {
                    warn(
                        """
                        Compilation: ${it.first.name}

                        Dependencies:
                        ${it.second.joinToString(separator = "\n") { it.stringCoordinates() }}

                    """.trimIndent()
                    )
                }
                warn("Such dependencies are not applicable for Kotlin/Native, consider changing the dependency type to 'implementation' or 'api'.")
            }
        }
    }
    // endregion.

    object NativeArtifactFormat {
        const val KLIB = "org.jetbrains.kotlin.klib"
    }

    companion object {
        const val INTEROP_GROUP = "interop"
        const val RUN_GROUP = "run"

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
                attributes.attribute(USAGE_ATTRIBUTE,  KotlinUsages.consumerApiUsage(target))
                description = "Dependencies for cinterop '${cinterop.name}' (compilation '${compilation.name}')."
            }
        }
    }
}

class KotlinNativeTargetWithTestsConfigurator(kotlinPluginVersion: String) :
    KotlinNativeTargetConfigurator<KotlinNativeTargetWithTests>(kotlinPluginVersion),
    KotlinTargetWithTestsConfigurator<KotlinNativeBinaryTestRun, KotlinNativeTargetWithTests> {

    override val testRunClass: Class<KotlinNativeBinaryTestRun>
        get() = KotlinNativeBinaryTestRun::class.java

    override fun createTestRun(name: String, target: KotlinNativeTargetWithTests): KotlinNativeBinaryTestRun =
        DefaultKotlinNativeTestRun(name, target).apply {
            val project = target.project

            val testTaskOrProvider = project.registerTask<KotlinNativeTest>(testTaskName) { testTask ->
                testTask.group = LifecycleBasePlugin.VERIFICATION_GROUP
                testTask.description = "Executes Kotlin/Native unit tests for target ${target.name}."
                testTask.targetName = target.name

                testTask.enabled = target.konanTarget.isCurrentHost

                testTask.workingDir = project.projectDir.absolutePath

                testTask.configureConventions()
            }

            executionTask = testTaskOrProvider

            setExecutionSourceFrom(target.binaries.getTest(NativeBuildType.DEBUG))

            project.kotlinTestRegistry.registerTestTask(testTaskOrProvider)
        }
}
