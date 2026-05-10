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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.internal.operations.BuildOperationListenerManager
import org.jetbrains.kotlin.compilerRunner.btapi.BuildSessionService
import org.jetbrains.kotlin.compilerRunner.maybeCreateCommonizerClasspathConfiguration
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.fus.BuildUidService
import org.jetbrains.kotlin.gradle.internal.KOTLIN_BUILD_TOOLS_API_COMPAT
import org.jetbrains.kotlin.gradle.internal.KOTLIN_BUILD_TOOLS_API_IMPL
import org.jetbrains.kotlin.gradle.internal.KOTLIN_COMPILER_EMBEDDABLE
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.attributes.setupAttributesMatchingStrategy
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpCompatibilityCheck.runAgpCompatibilityCheckIfAgpIsApplied
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpWithBuiltInKotlinAppliedCheck.runAgpWithBuiltInKotlinIfAppliedCheck
import org.jetbrains.kotlin.gradle.internal.diagnostics.AgpWithBuiltInKotlinAppliedCheck.runKmpAgpWithBuiltInKotlinIfAppliedCheck
import org.jetbrains.kotlin.gradle.internal.diagnostics.GradleCompatibilityCheck.runGradleCompatibilityCheck
import org.jetbrains.kotlin.gradle.internal.diagnostics.KotlinCompilerEmbeddableCheck.checkCompilerEmbeddableInClasspath
import org.jetbrains.kotlin.gradle.internal.properties.PropertiesBuildService
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.diagnostics.CompilerDiagnosticsProblemsReporter
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DefaultCompilerDiagnosticsProblemsReporter
import org.jetbrains.kotlin.gradle.plugin.diagnostics.DefaultProblemsReporter
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ProblemsReporter
import org.jetbrains.kotlin.gradle.plugin.internal.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFinishBuildService
import org.jetbrains.kotlin.gradle.plugin.statistics.BuildFusService
import org.jetbrains.kotlin.gradle.report.BuildMetricsService
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerArtifactTypeAttribute
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropKlibLibraryElements
import org.jetbrains.kotlin.gradle.targets.native.internal.CommonizerTargetAttribute
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeBundleBuildService
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool
import org.jetbrains.kotlin.gradle.tasks.publishing.addPgpSignatureHelpers
import org.jetbrains.kotlin.gradle.tasks.publishing.addPomValidationHelpers
import org.jetbrains.kotlin.gradle.tasks.publishing.addSigningValidationHelpers
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestsRegistry
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import javax.inject.Inject
import kotlin.reflect.KClass


internal abstract class BuildMetricsPlugin @Inject constructor(val buildOperationListenerManager: BuildOperationListenerManager) : Plugin<Project> {
    override fun apply(project: Project) {
        val buildMetricsService = BuildMetricsService.registerIfAbsent(project)
        buildMetricsService?.also { buildOperationListenerManager.addListener(it.get()) }
    }
}

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

        //BuildMetricsPlugin access variants so it should be applied after it initialization
        project.pluginManager.apply(BuildMetricsPlugin::class.java)

        BuildFinishedListenerService.registerIfAbsent(project)
        BuildSessionService.registerIfAbsent(project)

        val buildUidService = BuildUidService.registerIfAbsent(project.gradle)
        val buildFinishBuildService = BuildFinishBuildService.registerIfAbsent(project, buildUidService, pluginVersion)
        BuildFusService.registerIfAbsent(project, pluginVersion, buildUidService, buildFinishBuildService)
        PropertiesBuildService.registerIfAbsent(project)

        project.gradle.projectsEvaluated {
            whenBuildEvaluated(project)
        }

        addKotlinCompilerConfiguration(project)

        project.configurations.maybeCreateResolvable(PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            setInvisibleIfSupported()
            addGradlePluginMetadataAttributes(project)
        }

        val kotlinGradleBuildServices = KotlinGradleBuildServices.registerIfAbsent(project).get()
        if (!project.isProjectIsolationEnabled) {
            kotlinGradleBuildServices.detectKotlinPluginLoadedInMultipleProjects(project, pluginVersion)
        }

        KotlinNativeBundleBuildService.registerIfAbsent(project)

    }

    private fun addKotlinCompilerConfiguration(project: Project) {
        project
            .configurations
            .maybeCreateResolvable(COMPILER_CLASSPATH_CONFIGURATION_NAME)
            .defaultDependencies {
                @Suppress("DEPRECATION")
                if (project.kotlinPropertiesProvider.runKotlinCompilerViaBuildToolsApi.get()) {
                    it.add(
                        project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_BUILD_TOOLS_API_COMPAT:$pluginVersion")
                    )
                    it.add(
                        project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_BUILD_TOOLS_API_IMPL:$pluginVersion")
                    )
                } else {
                    it.add(
                        project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:${project.getKotlinPluginVersion()}")
                    )
                }
            }
        project
            .configurations
            .maybeCreateResolvable(BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME)
            .also {
                project.dependencies.add(it.name, "$KOTLIN_MODULE_GROUP:$KOTLIN_BUILD_TOOLS_API_COMPAT:$pluginVersion")
                project.dependencies.add(it.name, "$KOTLIN_MODULE_GROUP:$KOTLIN_BUILD_TOOLS_API_IMPL")
                it.withDependencies { dependencies ->
                    dependencies
                        .withType<ExternalDependency>()
                        .configureEach { dependency ->
                            if (dependency.name == KOTLIN_BUILD_TOOLS_API_COMPAT) {
                                // the compat layer is expected to be of a particular version regardless of the chosen compiler
                                return@configureEach
                            }
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
            ProblemsReporter.Factory::class,
            DefaultProblemsReporter.Factory()
        )

        factories.putIfAbsent(
            CompilerDiagnosticsProblemsReporter.Factory::class,
            DefaultCompilerDiagnosticsProblemsReporter.Factory()
        )

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

        factories.putIfAbsent(
            CopySpecAccessor.Factory::class,
            DefaultCopySpecAccessor.Factory(),
        )

        factories.putIfAbsent(
            BuildIdentifierAccessor.Factory::class,
            DefaultBuildIdentifierAccessor.Factory(),
        )

        factories.putIfAbsent(
            ProjectDependencyAccessor.Factory::class,
            DefaultProjectDependencyAccessor.Factory()
        )
    }

    protected fun setupAttributeMatchingStrategy(
        project: Project,
    ) = with(project.dependencies.attributesSchema) {
        KotlinPlatformType.setupAttributesMatchingStrategy(this)
        KotlinUsages.setupAttributesMatchingStrategy(
            this,
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
            setInvisibleIfSupported()
            isTransitive = false
            addGradlePluginMetadataAttributes(project)
        }
        project.maybeCreateCommonizerClasspathConfiguration()
        project.addPgpSignatureHelpers()
        project.addPomValidationHelpers()
        project.addSigningValidationHelpers()
        if (projectExtensionClass == KotlinAndroidProjectExtension::class) project.runAgpWithBuiltInKotlinIfAppliedCheck()
        if (projectExtensionClass == KotlinMultiplatformExtension::class) project.runKmpAgpWithBuiltInKotlinIfAppliedCheck()

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

abstract class AbstractKotlinPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinJvmPlugin()

    override val projectExtensionClass: KClass<out KotlinJvmProjectExtension>
        get() = KotlinJvmProjectExtension::class
}

abstract class AbstractKotlinAndroidPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinAndroidPlugin()

    override val projectExtensionClass: KClass<out KotlinAndroidProjectExtension>
        get() = KotlinAndroidProjectExtension::class
}

abstract class AbstractKotlinJsPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinJsPlugin()

    @Suppress("DEPRECATION_ERROR")
    override val projectExtensionClass: KClass<out KotlinJsProjectExtension>
        get() = KotlinJsProjectExtension::class
}

abstract class AbstractKotlinMultiplatformPluginWrapper : KotlinBasePluginWrapper() {
    @Suppress("DEPRECATION_ERROR")
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
