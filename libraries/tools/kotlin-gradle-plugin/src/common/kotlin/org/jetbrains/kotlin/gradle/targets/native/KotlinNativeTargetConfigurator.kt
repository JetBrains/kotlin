/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilationInfo.KPM
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.ReadyForExecution
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES
import org.jetbrains.kotlin.gradle.plugin.internal.artifactTypeAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.registerEmbedAndSignAppleFrameworkTask
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.*
import org.jetbrains.kotlin.gradle.targets.native.internal.commonizeCInteropTask
import org.jetbrains.kotlin.gradle.targets.native.internal.createCInteropApiElementsKlibArtifact
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropApiElementsConfiguration
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropDependencyConfiguration
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.testing.internal.configureConventions
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.testing.testTaskName
import org.jetbrains.kotlin.gradle.utils.Xcode
import org.jetbrains.kotlin.gradle.utils.klibModuleName
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

open class KotlinNativeTargetConfigurator<T : KotlinNativeTarget> : AbstractKotlinTargetConfigurator<T>(
    createTestCompilation = true
) {

    // region Task creation.
    private fun Project.createLinkTask(binary: NativeBinary) {
        // workaround for too late compilation compilerOptions creation
        // which leads to not able run project.afterEvaluate because of wrong context
        // this afterEvaluate comes from NativeCompilerOptions
        val compilationCompilerOptions = binary.compilation.compilerOptions
        val konanPropertiesBuildService = KonanPropertiesBuildService.registerIfAbsent(project)
        val linkTask = registerTask<KotlinNativeLink>(
            binary.linkTaskName, listOf(binary)
        ) {
            val target = binary.target
            it.group = BasePlugin.BUILD_GROUP
            it.description = "Links ${binary.outputKind.description} '${binary.name}' for a target '${target.name}'."
            it.enabled = binary.konanTarget.enabledOnCurrentHost
            it.konanPropertiesService.set(konanPropertiesBuildService)
            it.usesService(konanPropertiesBuildService)
            it.toolOptions.freeCompilerArgs.value(compilationCompilerOptions.options.freeCompilerArgs)
            it.toolOptions.freeCompilerArgs.addAll(providers.provider { PropertiesProvider(project).nativeLinkArgs })
        }


        if (binary !is TestExecutable) {
            tasks.named(binary.compilation.target.artifactsTaskName).dependsOn(linkTask)
            locateOrRegisterTask<Task>(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(linkTask)
        }

        if (binary is Framework) {
            createFrameworkArtifact(binary, linkTask)
        }
    }

    private fun Project.syncLanguageSettingsToLinkTask(binary: NativeBinary) {
        tasks.named(binary.linkTaskName, KotlinNativeLink::class.java).configure {
            // We propagate compilation free args to the link task for now (see KT-33717).
            val defaultLanguageSettings = binary.compilation.defaultSourceSet.languageSettings as? DefaultLanguageSettingsBuilder
            if (defaultLanguageSettings != null && defaultLanguageSettings.freeCompilerArgs.isNotEmpty()) {
                it.toolOptions.freeCompilerArgs.addAll(
                    defaultLanguageSettings.freeCompilerArgs
                )
            }
        }
    }

    private fun Project.createRunTask(binary: Executable) {
        val taskName = binary.runTaskName ?: return
        registerTask<Exec>(taskName) { exec ->
            exec.group = RUN_GROUP
            exec.description = "Executes Kotlin/Native executable ${binary.name} for target ${binary.target.name}"

            exec.enabled = binary.konanTarget.isCurrentHost

            exec.executable = binary.outputFile.absolutePath
            exec.workingDir = project.projectDir

            exec.onlyIf { binary.outputFile.exists() }
            exec.dependsOn(binary.linkTaskName)
        }
    }

    // FIXME support creating interop tasks for PM20
    private fun Project.createCInteropTasks(
        compilation: KotlinNativeCompilation,
        cinterops: NamedDomainObjectCollection<DefaultCInteropSettings>
    ) {
        val compilationInfo = KotlinCompilationInfo(compilation)
        cinterops.all { interop ->

            val params = CInteropProcess.Params(
                settings = interop,
                targetName = compilation.target.name,
                compilationName = compilation.name,
                konanTarget = compilation.konanTarget,
                baseKlibName = run {
                    val compilationPrefix = if (compilation.isMain()) project.name else compilation.name
                    "$compilationPrefix-cinterop-${interop.name}"
                },
                services = objects.newInstance()
            )

            val interopTask = registerTask<CInteropProcess>(interop.interopProcessingTaskName, listOf(params)) {
                it.destinationDir = provider { klibOutputDirectory(compilationInfo).resolve("cinterop") }
                it.group = INTEROP_GROUP
                it.description = "Generates Kotlin/Native interop library '${interop.name}' " +
                        "for compilation '${compilation.compilationName}'" +
                        "of target '${it.konanTarget.name}'."
                it.enabled = compilation.konanTarget.enabledOnCurrentHost
            }

            project.commonizeCInteropTask?.configure { commonizeCInteropTask ->
                commonizeCInteropTask.from((interopTask.get()))
            }

            val interopOutput = project.files(interopTask.map { it.outputFileProvider })
            with(compilation) {
                // Register the interop library as a dependency of the compilation to make IDE happy.
                project.dependencies.add(compileDependencyConfigurationName, interopOutput)
                if (isMain()) {
                    // Add interop library to special CInteropApiElements configuration
                    createCInteropApiElementsKlibArtifact(compilation.target, interop, interopTask)

                    // Add the interop library in publication.
                    createKlibArtifact(
                        compilationInfo = compilationInfo,
                        konanTarget = konanTarget,
                        artifactFile = interopTask.map { it.outputFile },
                        classifier = "cinterop-${interop.name}",
                        producingTask = interopTask,
                    )

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
    // endregion.

    // region Configuration.
    override fun configurePlatformSpecificModel(target: T) {
        configureBinaries(target)
        configureFrameworkExport(target)
        configureCInterops(target)

        if (target.konanTarget.family.isAppleFamily) {
            registerEmbedAndSignAppleFrameworkTasks(target)
        }

        if (PropertiesProvider(target.project).ignoreIncorrectNativeDependencies != true) {
            warnAboutIncorrectDependencies(target)
        }
    }

    override fun configureArchivesAndComponent(target: T): Unit = with(target.project) {
        registerTask<DefaultTask>(target.artifactsTaskName) {
            it.group = BasePlugin.BUILD_GROUP
            it.description = "Assembles outputs for target '${target.name}'."
        }
        target.compilations.all { createKlibCompilationTask(KotlinCompilationInfo(it), it.konanTarget) }

        val apiElements = configurations.getByName(target.apiElementsConfigurationName)

        apiElements.outgoing.attributes.attribute(artifactTypeAttribute, NativeArtifactFormat.KLIB)

        if (project.isKotlinGranularMetadataEnabled) {
            project.configurations.create(target.hostSpecificMetadataElementsConfigurationName) { configuration ->
                configuration.isCanBeConsumed = true
                configuration.isCanBeResolved = false

                configuration.extendsFrom(*apiElements.extendsFrom.toTypedArray())

                copyAttributes(from = apiElements.attributes, to = configuration.attributes)
                configuration.attributes.attribute(USAGE_ATTRIBUTE, objects.named(Usage::class.java, KotlinUsages.KOTLIN_METADATA))
            }
        }
    }

    protected fun configureCInterops(target: KotlinNativeTarget): Unit = with(target.project) {
        locateOrCreateCInteropApiElementsConfiguration(target)
        target.compilations.all { compilation ->
            createCInteropTasks(compilation, compilation.cinterops)
            compilation.cinterops.all { cinterop ->
                cinterop.dependencyFiles += locateOrCreateCInteropDependencyConfiguration(compilation)
            }
        }
    }

    protected fun configureBinaries(target: KotlinNativeTarget) {
        val project = target.project
        // Create link and run tasks.
        target.binaries.all {
            project.createLinkTask(it)
        }
        project.runOnceAfterEvaluated("Sync language settings for NativeLinkTask") {
            target.binaries.all { binary ->
                project.syncLanguageSettingsToLinkTask(binary)
            }
        }
        project.runOnceAfterEvaluated("Sync native compilation language settings to compiler options") {
            target.compilations.all { compilation ->
                compilation.compilerOptions.syncLanguageSettings(compilation.defaultSourceSet.languageSettings)
            }
        }

        target.binaries.withType(Executable::class.java).all {
            project.createRunTask(it)
        }

        target.binaries.prefixGroups.all { prefixGroup ->
            val linkGroupTask = project.locateOrRegisterTask<Task>(prefixGroup.linkTaskName) {
                it.group = BasePlugin.BUILD_GROUP
                it.description = "Links all binaries for target '${target.name}'."
            }
            prefixGroup.binaries.all {
                linkGroupTask.dependsOn(it.linkTaskName)
            }
        }

        // Create an aggregate link task for each compilation.
        target.compilations.all {
            project.registerTask<DefaultTask>(it.binariesTaskName) { task ->
                task.group = BasePlugin.BUILD_GROUP
                task.description = "Links all binaries for compilation '${it.name}' of target '${it.target.name}'."
            }
        }

        project.whenEvaluated {
            target.binaries.forEach { binary ->
                project.tasks.named(binary.compilation.binariesTaskName).configure { binariesTask ->
                    binariesTask.dependsOn(binary.linkTaskName)
                }
            }
        }

        /**
         * We create test binaries for all platforms but test runs only for:
         *  - host platforms: macosX64, linuxX64, mingwX64;
         *  - simulated platforms: iosX64, tvosX64, watchosX64.
         * See more in [KotlinNativeTargetWithTestsConfigurator] and its subclasses.
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
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
        }

        target.binaries.withType(AbstractNativeLibrary::class.java).all { framework ->
            project.configurations.maybeCreate(framework.exportConfigurationName).apply {
                isVisible = false
                isTransitive = false
                isCanBeConsumed = false
                isCanBeResolved = true
                usesPlatformOf(target)
                attributes.attribute(USAGE_ATTRIBUTE, KotlinUsages.consumerApiUsage(target))
                attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                description = "Dependenceis to be exported in framework ${framework.name} for target ${target.targetName}"
            }
        }
    }

    private fun registerEmbedAndSignAppleFrameworkTasks(target: KotlinNativeTarget) {
        val project = target.project
        target.binaries.withType(Framework::class.java).all { framework ->
            project.registerEmbedAndSignAppleFrameworkTask(framework)
        }
    }

    override fun defineConfigurationsForTarget(target: T) {
        super.defineConfigurationsForTarget(target)
        implementationToApiElements(target)
    }

    private fun warnAboutIncorrectDependencies(target: KotlinNativeTarget) = target.project.launchInStage(ReadyForExecution) {

        val compileOnlyDependencies = target.compilations.mapNotNull {
            val dependencies = project.configurations.getByName(it.compileOnlyConfigurationName).allDependencies
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
                warn(
                    """
                    Such dependencies are not applicable for Kotlin/Native, consider changing the dependency type to 'implementation' or 'api'.
                    To disable this warning, set the $KOTLIN_NATIVE_IGNORE_INCORRECT_DEPENDENCIES=true project property
                    """.trimIndent()
                )
            }
        }
    }
    // endregion.

    object NativeArtifactFormat {
        const val KLIB = "org.jetbrains.kotlin.klib"
        const val FRAMEWORK = "org.jetbrains.kotlin.framework"
    }

    companion object {
        const val INTEROP_GROUP = "interop"
        const val RUN_GROUP = "run"

        internal fun createKlibCompilationTask(
            compilationInfo: KotlinCompilationInfo,
            konanTarget: KonanTarget
        ): TaskProvider<KotlinNativeCompile> {
            val project = compilationInfo.project
            val compileTaskProvider = project.registerTask<KotlinNativeCompile>(
                compilationInfo.compileKotlinTaskName,
                listOf(
                    compilationInfo,
                    compilationInfo.compilerOptions.options as KotlinNativeCompilerOptions
                )
            ) {
                it.group = BasePlugin.BUILD_GROUP
                it.description = "Compiles a klibrary from the '${compilationInfo.compilationName}' " +
                        "compilation in target '${compilationInfo.targetDisambiguationClassifier}'."
                it.enabled = konanTarget.enabledOnCurrentHost

                it.destinationDirectory.set(project.klibOutputDirectory(compilationInfo).resolve("klib"))
                it.compilerOptions.moduleName.set(project.klibModuleName(it.baseName))
                val propertiesProvider = PropertiesProvider(project)
                if (propertiesProvider.useK2 == true) {
                    it.compilerOptions.useK2.set(true)
                }
                it.compilerOptions.useK2.disallowChanges()
            }

            compilationInfo.classesDirs.from(compileTaskProvider.map { it.outputFile })

            project.project.tasks.named(compilationInfo.compileAllTaskName).dependsOn(compileTaskProvider)

            if (compilationInfo.isMain) {
                if (compilationInfo is KotlinCompilationInfo.TCS && compilationInfo.compilation is KotlinNativeCompilation) {
                    project.project.tasks.named(compilationInfo.compilation.target.artifactsTaskName).dependsOn(compileTaskProvider)
                }

                project.project.tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME).dependsOn(compileTaskProvider)
            }
            val shouldAddCompileOutputsToElements =
                (compilationInfo is KPM && compilationInfo.compilationData.owner is GradleKpmVariant) ||
                        compilationInfo.isMain


            if (shouldAddCompileOutputsToElements) {
                createRegularKlibArtifact(compilationInfo, konanTarget, compileTaskProvider)
            }

            if (compilationInfo is KotlinCompilationInfo.TCS && compilationInfo.compilation is AbstractKotlinNativeCompilation) {
                // FIXME: support compiler plugins for PM20
                addCompilerPlugins(compilationInfo.compilation)
            }

            return compileTaskProvider
        }

        private fun Project.klibOutputDirectory(
            compilation: KotlinCompilationInfo
        ): File {
            val targetSubDirectory = compilation.targetDisambiguationClassifier?.let { "$it/" }.orEmpty()
            return buildDir.resolve("classes/kotlin/$targetSubDirectory${compilation.compilationName}")
        }

        private fun addCompilerPlugins(compilation: AbstractKotlinNativeCompilation) {
            val project = compilation.target.project

            project.whenEvaluated {
                SubpluginEnvironment
                    .loadSubplugins(project)
                    .addSubpluginOptions(project, compilation)

                compilation.compileKotlinTaskProvider.configure {
                    it.compilerPluginClasspath = compilation.configurations.pluginConfiguration
                }
            }
        }

        internal fun createRegularKlibArtifact(
            compilation: KotlinCompilationInfo,
            konanTarget: KonanTarget,
            compileTask: TaskProvider<out KotlinNativeCompile>
        ) = createKlibArtifact(compilation, konanTarget, compileTask.map { it.outputFile.get() }, null, compileTask)

        private fun createKlibArtifact(
            compilationInfo: KotlinCompilationInfo,
            konanTarget: KonanTarget,
            artifactFile: Provider<File>,
            classifier: String?,
            producingTask: TaskProvider<*>,
        ) {
            val project = compilationInfo.project
            if (!konanTarget.enabledOnCurrentHost) {
                return
            }

            val apiElementsName = when (compilationInfo) {
                is KPM -> (compilationInfo.compilationData.owner as GradleKpmVariant).apiElementsConfiguration.name
                is KotlinCompilationInfo.TCS -> compilationInfo.compilation.target.apiElementsConfigurationName
            }

            with(project.configurations.getByName(apiElementsName)) {
                val klibArtifact = project.project.artifacts.add(name, artifactFile) { artifact ->
                    artifact.name = compilationInfo.compilationName
                    artifact.extension = "klib"
                    artifact.type = "klib"
                    artifact.classifier = classifier
                    artifact.builtBy(producingTask)
                }
                project.project.extensions.getByType(DefaultArtifactPublicationSet::class.java).addCandidate(klibArtifact)
                artifacts.add(klibArtifact)
                attributes.attribute(project.artifactTypeAttribute, NativeArtifactFormat.KLIB)
            }
        }
    }
}

abstract class KotlinNativeTargetWithTestsConfigurator<
        TargetType : KotlinNativeTargetWithTests<TestRunType>,
        TestRunType : KotlinNativeBinaryTestRun,
        TaskType : KotlinNativeTest>(
) : KotlinNativeTargetConfigurator<TargetType>(),
    KotlinTargetWithTestsConfigurator<TestRunType, TargetType> {

    abstract val testTaskClass: Class<TaskType>

    abstract fun isTestTaskEnabled(target: TargetType): Boolean

    protected open fun configureTestTask(target: TargetType, testTask: TaskType) {
        testTask.group = LifecycleBasePlugin.VERIFICATION_GROUP
        testTask.description = "Executes Kotlin/Native unit tests for target ${target.name}."
        testTask.targetName = target.name

        testTask.enabled = isTestTaskEnabled(target)

        testTask.workingDir = target.project.projectDir.absolutePath

        testTask.configureConventions()
    }

    protected open fun configureTestRun(target: TargetType, testRun: AbstractKotlinNativeTestRun<TaskType>) {
        with(testRun) {
            val project = target.project

            val testTaskOrProvider = project.registerTask(testTaskName, testTaskClass) { testTask ->
                configureTestTask(target, testTask)
            }

            executionTask = testTaskOrProvider

            setExecutionSourceFrom(target.binaries.getTest(NativeBuildType.DEBUG))

            project.kotlinTestRegistry.registerTestTask(testTaskOrProvider)
        }
    }
}

class KotlinNativeTargetWithHostTestsConfigurator() :
    KotlinNativeTargetWithTestsConfigurator<
            KotlinNativeTargetWithHostTests,
            KotlinNativeHostTestRun,
            KotlinNativeHostTest>() {

    override val testTaskClass: Class<KotlinNativeHostTest>
        get() = KotlinNativeHostTest::class.java

    override val testRunClass: Class<KotlinNativeHostTestRun>
        get() = KotlinNativeHostTestRun::class.java

    override fun isTestTaskEnabled(target: KotlinNativeTargetWithHostTests): Boolean =
        target.konanTarget.isCurrentHost

    override fun createTestRun(
        name: String,
        target: KotlinNativeTargetWithHostTests
    ): KotlinNativeHostTestRun =
        DefaultHostTestRun(name, target).apply { configureTestRun(target, this) }
}

class KotlinNativeTargetWithSimulatorTestsConfigurator :
    KotlinNativeTargetWithTestsConfigurator<
            KotlinNativeTargetWithSimulatorTests,
            KotlinNativeSimulatorTestRun,
            KotlinNativeSimulatorTest>() {

    override val testTaskClass: Class<KotlinNativeSimulatorTest>
        get() = KotlinNativeSimulatorTest::class.java

    override val testRunClass: Class<KotlinNativeSimulatorTestRun>
        get() = KotlinNativeSimulatorTestRun::class.java

    override fun isTestTaskEnabled(target: KotlinNativeTargetWithSimulatorTests): Boolean =
        HostManager.hostIsMac && HostManager.host.architecture == target.konanTarget.architecture

    override fun configureTestTask(target: KotlinNativeTargetWithSimulatorTests, testTask: KotlinNativeSimulatorTest) {
        super.configureTestTask(target, testTask)
        if (Xcode != null) {
            val deviceIdProvider = testTask.project.provider {
                Xcode.getDefaultTestDeviceId(target.konanTarget)
                    ?: error("Xcode does not support simulator tests for ${target.konanTarget.name}. Check that requested SDK is installed.")
            }
            testTask.device.convention(deviceIdProvider).finalizeValueOnRead()
        }
        testTask.standalone.convention(true).finalizeValueOnRead()
    }

    override fun createTestRun(
        name: String,
        target: KotlinNativeTargetWithSimulatorTests
    ): KotlinNativeSimulatorTestRun =
        DefaultSimulatorTestRun(name, target).apply { configureTestRun(target, this) }
}
