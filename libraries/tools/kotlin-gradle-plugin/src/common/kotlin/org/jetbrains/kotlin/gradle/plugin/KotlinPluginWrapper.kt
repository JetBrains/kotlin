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
import org.gradle.api.model.ObjectFactory
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.compilerRunner.registerCommonizerClasspathConfigurationIfNecessary
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.KOTLIN_COMPILER_EMBEDDABLE
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.internal.MavenPluginConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20GradlePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.plugin.sources.kpm.FragmentMappedKotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatsService
import org.jetbrains.kotlin.gradle.report.BuildMetricsReporterService
import org.jetbrains.kotlin.gradle.report.HttpReportService
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.addNpmDependencyExtension
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropKlibLibraryElements
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestsRegistry
import org.jetbrains.kotlin.gradle.tooling.registerBuildKotlinToolingMetadataTask
import org.jetbrains.kotlin.gradle.utils.addGradlePluginMetadataAttributes
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility
import org.jetbrains.kotlin.gradle.utils.getOrPut
import org.jetbrains.kotlin.gradle.utils.loadPropertyFromResources
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheck
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import kotlin.reflect.KClass

abstract class KotlinBasePluginWrapper : Plugin<Project> {

    open val projectExtensionClass: KClass<out KotlinTopLevelExtension> get() = KotlinProjectExtension::class

    internal open fun kotlinSourceSetFactory(project: Project): NamedDomainObjectFactory<KotlinSourceSet> =
        if (PropertiesProvider(project).experimentalKpmModelMapping)
            FragmentMappedKotlinSourceSetFactory(project)
        else DefaultKotlinSourceSetFactory(project)

    override fun apply(project: Project) {
        val kotlinPluginVersion = project.getKotlinPluginVersion()

        val statisticsReporter = KotlinBuildStatsService.getOrCreateInstance(project)
        statisticsReporter?.report(StringMetrics.KOTLIN_COMPILER_VERSION, kotlinPluginVersion)

        checkGradleCompatibility()

        project.gradle.projectsEvaluated {
            whenBuildEvaluated(project)
        }

        addKotlinCompilerConfiguration(project)

        project.configurations.maybeCreate(PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            isVisible = false
            isCanBeConsumed = false
            addGradlePluginMetadataAttributes(project)
        }

        project.configurations.maybeCreate(NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME).apply {
            isVisible = false
            isCanBeConsumed = false
            isTransitive = false
            addGradlePluginMetadataAttributes(project)
        }
        project.registerCommonizerClasspathConfigurationIfNecessary()

        project.registerDefaultVariantImplementations()

        KotlinGradleBuildServices.registerIfAbsent(project, kotlinPluginVersion).get()

        KotlinGradleBuildServices.detectKotlinPluginLoadedInMultipleProjects(project, kotlinPluginVersion)

        val buildMetricReporter = BuildMetricsReporterService.registerIfAbsent(project)

        buildMetricReporter?.also { BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(it) }

        HttpReportService.registerIfAbsent(project, kotlinPluginVersion)
            ?.also { BuildEventsListenerRegistryHolder.getInstance(project).listenerRegistry.onTaskCompletion(it) }

        project.tasks.withType(AbstractKotlinCompile::class.java).configureEach {
            if (buildMetricReporter != null) {
                it.buildMetricsReporterService.set(buildMetricReporter)
            }
        }

        project.createKotlinExtension(projectExtensionClass).apply {
            coreLibrariesVersion = kotlinPluginVersion

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

        plugin.apply(project)

        project.addNpmDependencyExtension()

        project.registerBuildKotlinToolingMetadataTask()
    }

    private fun Project.registerDefaultVariantImplementations() {
        val factories = VariantImplementationFactories.get(project.gradle)
        factories.putIfAbsent(
            MavenPluginConfigurator.MavenPluginConfiguratorVariantFactory::class,
            MavenPluginConfigurator.DefaultMavenPluginConfiguratorVariantFactory()
        )
    }

    private fun addKotlinCompilerConfiguration(project: Project) {
        project
            .configurations
            .maybeCreate(COMPILER_CLASSPATH_CONFIGURATION_NAME)
            .defaultDependencies {
                it.add(
                    project.dependencies.create("$KOTLIN_MODULE_GROUP:$KOTLIN_COMPILER_EMBEDDABLE:${project.getKotlinPluginVersion()}")
                )
            }
        project
            .tasks
            .withType(AbstractKotlinCompile::class.java)
            .configureEach { task ->
                task.defaultCompilerClasspath.setFrom(
                    project.configurations.named(COMPILER_CLASSPATH_CONFIGURATION_NAME)
                )
            }
    }

    open fun whenBuildEvaluated(project: Project) {
    }

    internal open fun createTestRegistry(project: Project) = KotlinTestsRegistry(project)

    private fun setupAttributeMatchingStrategy(project: Project) = with(project.dependencies.attributesSchema) {
        KotlinPlatformType.setupAttributesMatchingStrategy(this)
        KotlinUsages.setupAttributesMatchingStrategy(project, this)
        KotlinJsCompilerAttribute.setupAttributesMatchingStrategy(project.dependencies.attributesSchema)
        ProjectLocalConfigurations.setupAttributesMatchingStrategy(this)
        CInteropKlibLibraryElements.setupAttributesMatchingStrategy(this)
    }

    internal abstract fun getPlugin(
        project: Project,
    ): Plugin<Project>
}

abstract class AbstractKotlinPluginWrapper(
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinJvmProjectExtension>
        get() = KotlinJvmProjectExtension::class
}

abstract class AbstractKotlinCommonPluginWrapper(
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinCommonPlugin(registry)

    override val projectExtensionClass: KClass<out KotlinCommonProjectExtension>
        get() = KotlinCommonProjectExtension::class
}

abstract class AbstractKotlinAndroidPluginWrapper(
    protected val registry: ToolingModelBuilderRegistry
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
    protected val registry: ToolingModelBuilderRegistry
) : KotlinBasePluginWrapper() {

    @Suppress("DEPRECATION_ERROR")
    override fun getPlugin(project: Project): Plugin<Project> =
        Kotlin2JsPlugin(registry)

    override val projectExtensionClass: KClass<out Kotlin2JsProjectExtension>
        get() = Kotlin2JsProjectExtension::class
}

abstract class AbstractKotlinJsPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        KotlinJsPlugin(project.getKotlinPluginVersion())

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

    override val projectExtensionClass: KClass<out KotlinMultiplatformExtension>
        get() = KotlinMultiplatformExtension::class

    override fun whenBuildEvaluated(project: Project) {
        project.runMissingAndroidTargetProjectConfigurationHealthCheck()
        project.runMissingKotlinTargetsProjectConfigurationHealthCheck()
        project.runDisabledCInteropCommonizationOnHmppProjectConfigurationHealthCheck()
    }
}

abstract class AbstractKotlinPm20PluginWrapper(
    private val objectFactory: ObjectFactory
) : KotlinBasePluginWrapper() {
    override fun getPlugin(project: Project): Plugin<Project> =
        objectFactory.newInstance(KotlinPm20GradlePlugin::class.java)

    override val projectExtensionClass: KClass<out KotlinPm20ProjectExtension>
        get() = KotlinPm20ProjectExtension::class
}

fun Project.getKotlinPluginVersion(): String {
    if (!kotlinPluginVersionFromResources.isInitialized()) {
        project.logger.kotlinDebug("Loading version information")
        project.logger.kotlinDebug("Found project version [${kotlinPluginVersionFromResources.value}")
    }
    return kotlinPluginVersionFromResources.value
}

@ExperimentalKotlinGradlePluginApi
val Project.kotlinToolingVersion: KotlinToolingVersion
    get() = extensions.extraProperties.getOrPut("kotlinToolingVersion") {
        KotlinToolingVersion(getKotlinPluginVersion())
    }


private fun loadKotlinPluginVersionFromResourcesOf(any: Any) =
    any.loadPropertyFromResources("project.properties", "project.version")

private val kotlinPluginVersionFromResources = lazy {
    loadKotlinPluginVersionFromResourcesOf(object {})
}
