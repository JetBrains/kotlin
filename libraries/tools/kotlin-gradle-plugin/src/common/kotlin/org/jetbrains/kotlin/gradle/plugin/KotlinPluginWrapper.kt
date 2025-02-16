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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.compilerRunner.maybeCreateCommonizerClasspathConfiguration
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.internal.KOTLIN_BUILD_TOOLS_API_IMPL
import org.jetbrains.kotlin.gradle.internal.KOTLIN_COMPILER_EMBEDDABLE
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.attributes.setupAttributesMatchingStrategy
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpCompatibilityCheck.runAgpCompatibilityCheckIfAgpIsApplied
import org.jetbrains.kotlin.gradle.internal.diagnostics.GradleCompatibilityCheck.runGradleCompatibilityCheck
import org.jetbrains.kotlin.gradle.internal.diagnostics.KotlinCompilerEmbeddableCheck.checkCompilerEmbeddableInClasspath
import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.internal.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.initSwiftExportClasspathConfigurations
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve.KotlinTargetResourcesResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerArtifactTypeAttribute
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropKlibLibraryElements
import org.jetbrains.kotlin.gradle.targets.native.internal.CommonizerTargetAttribute
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestsRegistry
import org.jetbrains.kotlin.gradle.utils.*
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
        project.checkCompilerEmbeddableInClasspath()
        project.registerDefaultVariantImplementations()
        project.runGradleCompatibilityCheck()
        project.runAgpCompatibilityCheckIfAgpIsApplied()
        BuildFinishedListenerService.registerIfAbsent(project)

        val buildUidService = BuildUidService.registerIfAbsent(project)
        if (project.kotlinPropertiesProvider.enableFusMetricsCollection) {
            BuildFusService.registerIfAbsent(project, pluginVersion, buildUidService)
        }
        PropertiesBuildService.registerIfAbsent(project)

        project.gradle.projectsEvaluated {
            whenBuildEvaluated(project)
        }

        addKotlinCompilerConfiguration(project)

        project.configurations.maybeCreateResolvable(PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            isVisible = false
            addGradlePluginMetadataAttributes(project)
        }

        val kotlinGradleBuildServices = KotlinGradleBuildServices.registerIfAbsent(project).get()
        if (!project.isProjectIsolationEnabled) {
            kotlinGradleBuildServices.detectKotlinPluginLoadedInMultipleProjects(project, pluginVersion)
        }

        BuildMetricsService.registerIfAbsent(project)
        KotlinNativeBundleBuildService.registerIfAbsent(project)
    }

    private fun addKotlinCompilerConfiguration(project: Project) {
        project
            .configurations
            .maybeCreateResolvable(COMPILER_CLASSPATH_CONFIGURATION_NAME)
            .defaultDependencies {
                it.add(
                    project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:${project.getKotlinPluginVersion()}")
                )
            }
        project
            .configurations
            .maybeCreateResolvable(BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME)
            .also {
                project.dependencies.add(it.name, "$KOTLIN_MODULE_GROUP:$KOTLIN_BUILD_TOOLS_API_IMPL")
                it.withDependencies { dependencies ->
                    dependencies
                        .withType<ExternalDependency>()
                        .configureEach { dependency ->
                            dependency.version { versionConstraint ->
                                versionConstraint.strictly(project.kotlinExtensionOrNull?.compilerVersion?.get() ?: pluginVersion)
                            }
                        }
                }
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
            ProjectIsolationStartParameterAccessor.Factory::class,
            DefaultProjectIsolationStartParameterAccessor.Factory()
        )

        factories.putIfAbsent(
            CompatibilityConventionRegistrar.Factory::class,
            DefaultCompatibilityConventionRegistrar.Factory()
        )

        factories.putIfAbsent(
            ConfigurationCacheStartParameterAccessor.Factory::class,
            DefaultConfigurationCacheStartParameterAccessorVariantFactory()
        )

        factories.putIfAbsent(
            MavenPublicationComponentAccessor.Factory::class,
            DefaultMavenPublicationComponentAccessorFactory()
        )

        factories.putIfAbsent(
            JavaExecTaskParametersCompatibility.Factory::class,
            DefaultJavaExecTaskParametersCompatibility.Factory()
        )
    }

    protected fun setupAttributeMatchingStrategy(
        project: Project,
        isKotlinGranularMetadata: Boolean = project.isKotlinGranularMetadataEnabled,
    ) = with(project.dependencies.attributesSchema) {
        KotlinPlatformType.setupAttributesMatchingStrategy(this)
        KotlinUsages.setupAttributesMatchingStrategy(
            this,
            isKotlinGranularMetadata,
            project.kotlinPropertiesProvider.mppResourcesResolutionStrategy == KotlinTargetResourcesResolutionStrategy.ResourcesConfiguration
        )
        ProjectLocalConfigurations.setupAttributesMatchingStrategy(this)

        project.whenJsOrMppEnabled {
            KotlinJsCompilerAttribute.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
            KotlinWasmTargetAttribute.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
            if (project.kotlinPropertiesProvider.useNonPackedKlibs) {
                KlibPackaging.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
            }
        }

        project.whenMppEnabled {
            CInteropKlibLibraryElements.setupAttributesMatchingStrategy(this)
            CommonizerTargetAttribute.setupAttributesMatchingStrategy(this)
            CInteropCommonizerArtifactTypeAttribute.setupTransform(project)
        }
    }

    open fun whenBuildEvaluated(project: Project) {
    }
}


abstract class KotlinBasePluginWrapper : DefaultKotlinBasePlugin() {

    open val projectExtensionClass: KClass<out KotlinBaseExtension> get() = KotlinProjectExtension::class

    abstract val pluginVariant: String

    override fun apply(project: Project) {
        super.apply(project)
        project.logger.info("Using Kotlin Gradle Plugin $pluginVariant variant")

        project.configurations.maybeCreateResolvable(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            isVisible = false
            isTransitive = false
            addGradlePluginMetadataAttributes(project)
        }
        project.maybeCreateCommonizerClasspathConfiguration()
        project.initSwiftExportClasspathConfigurations()

        project.createKotlinExtension(projectExtensionClass).apply {
            coreLibrariesVersion = pluginVersion
        }

        project.extensions.add(KotlinTestsRegistry.PROJECT_EXTENSION_NAME, createTestRegistry(project))

        val plugin = getPlugin(project)

        setupAttributeMatchingStrategy(project)

        project.registerKotlinPluginExtensions()

        project.startKotlinPluginLifecycle()

        plugin.apply(project)

        project.runKotlinProjectSetupActions()
    }

    internal open fun createTestRegistry(project: Project) = KotlinTestsRegistry(project)

    internal abstract fun getPlugin(
        project: Project,
    ): Plugin<Project>
}

abstract class AbstractKotlinPluginWrapper(
    protected val registry: ToolingModelBuilderRegistry,
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinJvmPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinJvmProjectExtension>
        get() = KotlinJvmProjectExtension::class
}

abstract class AbstractKotlinAndroidPluginWrapper(
    protected val registry: ToolingModelBuilderRegistry,
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinAndroidPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinAndroidProjectExtension>
        get() = KotlinAndroidProjectExtension::class
}

abstract class AbstractKotlinJsPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinJsPlugin()

    override val projectExtensionClass: KClass<out KotlinJsProjectExtension>
        get() = KotlinJsProjectExtension::class

    override fun whenBuildEvaluated(project: Project) = project.runProjectConfigurationHealthCheck {
        val isJsTargetUninitialized = !(project.kotlinExtension as KotlinJsProjectExtension).targetFuture.isCompleted

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
    @Suppress("DEPRECATION")
    override fun getPlugin(project: Project): Plugin<Project> = KotlinMultiplatformPlugin()

    override val projectExtensionClass: KClass<out KotlinMultiplatformExtension>
        get() = KotlinMultiplatformExtension::class
}

fun Project.getKotlinPluginVersion() = getKotlinPluginVersion(project.logger)

fun getKotlinPluginVersion(logger: Logger): String {
    if (!kotlinPluginVersionFromResources.isInitialized()) {
        logger.kotlinDebug("Loading version information")
        logger.kotlinDebug("Found project version [${kotlinPluginVersionFromResources.value}]")
    }
    return kotlinPluginVersionFromResources.value
}

val Project.kotlinToolingVersion: KotlinToolingVersion by projectStoredProperty {
    KotlinToolingVersion(getKotlinPluginVersion())
}

private fun loadKotlinPluginVersionFromResourcesOf(any: Any) =
    any.loadPropertyFromResources("project.properties", "project.version")

private val kotlinPluginVersionFromResources = lazy {
    loadKotlinPluginVersionFromResourcesOf(object {})
}
