/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.compilerRunner.maybeCreateCommonizerClasspathConfiguration
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.KOTLIN_BUILD_TOOLS_API_IMPL
import org.jetbrains.kotlin.gradle.internal.KOTLIN_COMPILER_EMBEDDABLE
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformAndroidGradlePluginCompatibilityHealthCheck.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticRenderingOptions
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollectorProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.launchKotlinGradleProjectCheckers
import org.jetbrains.kotlin.gradle.plugin.internal.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20GradlePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFlowService
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.js.nodejs.DefaultUnameExecutorVariantFactory
import org.jetbrains.kotlin.gradle.targets.js.nodejs.UnameExecutor
import org.jetbrains.kotlin.gradle.targets.js.npm.addNpmDependencyExtension
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropKlibLibraryElements
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerArtifactTypeAttribute
import org.jetbrains.kotlin.gradle.targets.native.internal.CommonizerTargetAttribute
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestsRegistry
import org.jetbrains.kotlin.gradle.tooling.registerBuildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import kotlin.reflect.KClass

/**
 * Base Kotlin plugin that is responsible for creating basic build services, configurations,
 * and other setup that is common for all Kotlin projects.
 */
abstract class DefaultKotlinBasePlugin : KotlinBasePlugin {

    private val logger = Logging.getLogger(DefaultKotlinBasePlugin::class.java)
    override val pluginVersion: String = getKotlinPluginVersion(logger)

    override fun apply(project: Project) {
        checkGradleCompatibility()

        project.registerDefaultVariantImplementations()
        KotlinBuildStatsService.getOrCreateInstance(project)?.apply {
            report(StringMetrics.KOTLIN_COMPILER_VERSION, pluginVersion)
        }
        BuildFlowService.registerIfAbsent(project)

        project.gradle.projectsEvaluated {
            whenBuildEvaluated(project)
        }

        addKotlinCompilerConfiguration(project)


        project.configurations.maybeCreate(PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
            addGradlePluginMetadataAttributes(project)
        }

        val kotlinGradleBuildServices = KotlinGradleBuildServices.registerIfAbsent(project.gradle).get()
        if (!project.isProjectIsolationEnabled) {
            kotlinGradleBuildServices.detectKotlinPluginLoadedInMultipleProjects(project, pluginVersion)
        }

        BuildMetricsService.registerIfAbsent(project)
    }

    private fun addKotlinCompilerConfiguration(project: Project) {
        project
            .configurations
            .maybeCreate(COMPILER_CLASSPATH_CONFIGURATION_NAME)
            .markResolvable()
            .defaultDependencies {
                it.add(
                    project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:${project.getKotlinPluginVersion()}")
                )
            }
        project
            .configurations
            .maybeCreate(BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME)
            .markResolvable()
            .defaultDependencies {
                it.add(
                    project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_BUILD_TOOLS_API_IMPL:${project.getKotlinPluginVersion()}")
                )
            }
        project
            .tasks
            .withType(AbstractKotlinCompileTool::class.java)
            .configureEach { task ->
                task.defaultCompilerClasspath.setFrom(
                    {
                        val classpathConfiguration = when (task.runViaBuildToolsApi.get()) {
                            true -> BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME
                            false -> COMPILER_CLASSPATH_CONFIGURATION_NAME
                        }
                        project.configurations.named(classpathConfiguration)
                    }
                )
            }
    }

    private fun Project.registerDefaultVariantImplementations() {
        val factories = VariantImplementationFactoriesConfigurator.get(project.gradle)
        factories.putIfAbsent(
            MavenPluginConfigurator.MavenPluginConfiguratorVariantFactory::class,
            MavenPluginConfigurator.DefaultMavenPluginConfiguratorVariantFactory()
        )

        factories.putIfAbsent(
            JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory::class,
            DefaultJavaSourceSetsAccessorVariantFactory()
        )

        factories.putIfAbsent(
            BasePluginConfiguration.BasePluginConfigurationVariantFactory::class,
            DefaultBasePluginConfigurationVariantFactory()
        )

        factories.putIfAbsent(
            IdeaSyncDetector.IdeaSyncDetectorVariantFactory::class,
            DefaultIdeaSyncDetectorVariantFactory()
        )

        factories.putIfAbsent(
            ConfigurationTimePropertiesAccessor.ConfigurationTimePropertiesAccessorVariantFactory::class,
            DefaultConfigurationTimePropertiesAccessorVariantFactory()
        )

        factories.putIfAbsent(
            MppTestReportHelper.MppTestReportHelperVariantFactory::class,
            DefaultMppTestReportHelperVariantFactory()
        )

        factories.putIfAbsent(
            KotlinTestReportCompatibilityHelper.KotlinTestReportCompatibilityHelperVariantFactory::class,
            DefaultKotlinTestReportCompatibilityHelperVariantFactory()
        )

        factories.putIfAbsent(
            ArtifactTypeAttributeAccessor.ArtifactTypeAttributeAccessorVariantFactory::class,
            DefaultArtifactTypeAttributeAccessorVariantFactory()
        )

        factories.putIfAbsent(
            ProjectIsolationStartParameterAccessor.Factory::class,
            DefaultProjectIsolationStartParameterAccessorVariantFactory()
        )

        factories.putIfAbsent(
            CompatibilityConventionRegistrar.Factory::class,
            DefaultCompatibilityConventionRegistrar.Factory()
        )

        factories.putIfAbsent(
            UnameExecutor.UnameExecutorVariantFactory::class,
            DefaultUnameExecutorVariantFactory()
        )
    }

    protected fun setupAttributeMatchingStrategy(
        project: Project,
        isKotlinGranularMetadata: Boolean = project.isKotlinGranularMetadataEnabled,
    ) = with(project.dependencies.attributesSchema) {
        KotlinPlatformType.setupAttributesMatchingStrategy(this)
        KotlinUsages.setupAttributesMatchingStrategy(this, isKotlinGranularMetadata)
        KotlinJsCompilerAttribute.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
        KotlinWasmTargetAttribute.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
        ProjectLocalConfigurations.setupAttributesMatchingStrategy(this)
        CInteropKlibLibraryElements.setupAttributesMatchingStrategy(this)
        CommonizerTargetAttribute.setupAttributesMatchingStrategy(this)
        CInteropCommonizerArtifactTypeAttribute.setupTransform(project)
    }

    open fun whenBuildEvaluated(project: Project) {
    }
}


abstract class KotlinBasePluginWrapper : DefaultKotlinBasePlugin() {

    open val projectExtensionClass: KClass<out KotlinTopLevelExtension> get() = KotlinProjectExtension::class

    abstract val pluginVariant: String

    internal open fun kotlinSourceSetFactory(project: Project): NamedDomainObjectFactory<KotlinSourceSet> =
        DefaultKotlinSourceSetFactory(project)

    override fun apply(project: Project) {
        super.apply(project)

        project.logger.info("Using Kotlin Gradle Plugin $pluginVariant variant")

        project.configurations.maybeCreate(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            isVisible = false
            isCanBeConsumed = false
            isTransitive = false
            addGradlePluginMetadataAttributes(project)
        }
        project.maybeCreateCommonizerClasspathConfiguration()

        project.createKotlinExtension(projectExtensionClass).apply {
            coreLibrariesVersion = pluginVersion

            fun kotlinSourceSetContainer(factory: NamedDomainObjectFactory<KotlinSourceSet>) =
                project.container(KotlinSourceSet::class.java, factory)

            val topLevelExtension = project.topLevelExtension
            if (topLevelExtension is KotlinProjectExtension) {
                project.kotlinExtension.sourceSets = kotlinSourceSetContainer(kotlinSourceSetFactory(project))
            }
        }

        project.extensions.add(KotlinTestsRegistry.PROJECT_EXTENSION_NAME, createTestRegistry(project))

        val plugin = getPlugin(project)

        setupAttributeMatchingStrategy(project)

        project.startKotlinPluginLifecycle()

        plugin.apply(project)

        project.addNpmDependencyExtension()

        project.registerBuildKotlinToolingMetadataTask()

        project.setupDiagnosticsChecksAndReporting()
    }

    internal open fun createTestRegistry(project: Project) = KotlinTestsRegistry(project)

    internal abstract fun getPlugin(
        project: Project,
    ): Plugin<Project>
}

private fun Project.setupDiagnosticsChecksAndReporting() {
    val collectorProvider = kotlinToolingDiagnosticsCollectorProvider
    val diagnosticRenderingOptions = ToolingDiagnosticRenderingOptions.forProject(this)

    // Setup reporting from tasks
    tasks.withType(UsesKotlinToolingDiagnostics::class.java).configureEach {
        it.usesService(collectorProvider)
        it.toolingDiagnosticsCollector.value(collectorProvider)
        it.diagnosticRenderingOptions.set(diagnosticRenderingOptions)
    }

    // Launch checkers. Note that they are invoked eagerly to give them a fine-grained
    // control over the lifecycle
    launchKotlinGradleProjectCheckers()

    // Setup a task that will abort the build if errors will be reported. This task should be the first in the taskgraph
    project.locateOrRegisterCheckKotlinGradlePluginErrorsTask()

    // Schedule diagnostics rendering
    launch {
        configurationResult.await()
        renderReportedDiagnostics(
            collectorProvider.get().getDiagnosticsForProject(project),
            logger,
            diagnosticRenderingOptions
        )
    }

    // Schedule switching of Collector to transparent mode, so that any diagnostics reported
    // after projects are evaluated will be transparently rendered right away instead of being
    // silently swallowed
    gradle.projectsEvaluated {
        collectorProvider.get().switchToTransparentMode()
    }
}

abstract class AbstractKotlinPluginWrapper(
    protected val registry: ToolingModelBuilderRegistry,
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinJvmPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinJvmProjectExtension>
        get() = KotlinJvmProjectExtension::class
}

abstract class AbstractKotlinCommonPluginWrapper(
    protected val registry: ToolingModelBuilderRegistry,
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinCommonPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinCommonProjectExtension>
        get() = KotlinCommonProjectExtension::class
}

abstract class AbstractKotlinAndroidPluginWrapper(
    protected val registry: ToolingModelBuilderRegistry,
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinAndroidPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinAndroidProjectExtension>
        get() = KotlinAndroidProjectExtension::class
}

@Deprecated(
    message = "Should be removed with JS platform plugin",
    level = DeprecationLevel.ERROR
)
abstract class AbstractKotlin2JsPluginWrapper(
    protected val registry: ToolingModelBuilderRegistry,
) : KotlinBasePluginWrapper() {

    @Suppress("DEPRECATION_ERROR")
    override fun getPlugin(project: Project): Plugin<Project> =
        Kotlin2JsPlugin(registry)

    override val projectExtensionClass: KClass<out Kotlin2JsProjectExtension>
        get() = Kotlin2JsProjectExtension::class
}

abstract class AbstractKotlinJsPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinJsPlugin()

    override val projectExtensionClass: KClass<out KotlinJsProjectExtension>
        get() = KotlinJsProjectExtension::class

    override fun whenBuildEvaluated(project: Project) = project.runProjectConfigurationHealthCheck {
        val isJsTargetUninitialized = (project.kotlinExtension as KotlinJsProjectExtension)
            ._target == null

        if (isJsTargetUninitialized) {
            throw GradleException(
                """
                Please initialize the Kotlin/JS target in '${project.name} (${project.path})'. Use:
                kotlin {
                    js {
                        // To build distributions and run tests for browser or Node.js use one or both of:
                        browser()
                        nodejs()
                    }
                }
                Read more https://kotlinlang.org/docs/reference/js-project-setup.html
                """.trimIndent()
            )
        }
    }

    override fun createTestRegistry(project: Project) = KotlinTestsRegistry(project, "test")
}

abstract class AbstractKotlinMultiplatformPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinMultiplatformPlugin()

    override fun apply(project: Project) {
        super.apply(project)
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied()
    }

    override val projectExtensionClass: KClass<out KotlinMultiplatformExtension>
        get() = KotlinMultiplatformExtension::class
}

abstract class AbstractKotlinPm20PluginWrapper(
    private val objectFactory: ObjectFactory,
) : KotlinBasePluginWrapper() {
    override fun apply(project: Project) {
        super.apply(project)
        project.runMultiplatformAndroidGradlePluginCompatibilityHealthCheckWhenAndroidIsApplied()
    }

    override fun getPlugin(project: Project): Plugin<Project> =
        objectFactory.newInstance(KotlinPm20GradlePlugin::class.java)

    override val projectExtensionClass: KClass<out KotlinPm20ProjectExtension>
        get() = KotlinPm20ProjectExtension::class
}

fun Project.getKotlinPluginVersion() = getKotlinPluginVersion(project.logger)

fun getKotlinPluginVersion(logger: Logger): String {
    if (!kotlinPluginVersionFromResources.isInitialized()) {
        logger.kotlinDebug("Loading version information")
        logger.kotlinDebug("Found project version [${kotlinPluginVersionFromResources.value}]")
    }
    return kotlinPluginVersionFromResources.value
}

val Project.kotlinToolingVersion: KotlinToolingVersion
    get() = extensions.extraProperties.getOrPut("kotlinToolingVersion") {
        KotlinToolingVersion(getKotlinPluginVersion())
    }


private fun loadKotlinPluginVersionFromResourcesOf(any: Any) =
    any.loadPropertyFromResources("project.properties", "project.version")

private val kotlinPluginVersionFromResources = lazy {
    loadKotlinPluginVersionFromResourcesOf(object {})
}
