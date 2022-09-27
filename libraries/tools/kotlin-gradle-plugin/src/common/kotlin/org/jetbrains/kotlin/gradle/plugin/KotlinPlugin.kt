/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.*
import org.gradle.api.*
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.model.builder.KotlinModelBuilder
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.configuration.*
import org.jetbrains.kotlin.gradle.utils.*

const val PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerPluginClasspath"
const val NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinNativeCompilerPluginClasspath"
internal const val COMPILER_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerClasspath"
internal const val KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME = "kotlinKlibCommonizerClasspath"

val KOTLIN_DSL_NAME = "kotlin"

@Deprecated("Should be removed with 'platform.js' plugin removal")
val KOTLIN_JS_DSL_NAME = "kotlin2js"
val KOTLIN_OPTIONS_DSL_NAME = "kotlinOptions"


internal open class KotlinPlugin(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    companion object {
        private const val targetName = "" // use empty suffix for the task names
    }

    override fun buildSourceSetProcessor(project: Project, compilation: AbstractKotlinCompilation<*>) =
        Kotlin2JvmSourceSetProcessor(tasksProvider, compilation)

    override fun apply(project: Project) {
        @Suppress("UNCHECKED_CAST")
        val target = (project.objects.newInstance(
            KotlinWithJavaTarget::class.java,
            project,
            KotlinPlatformType.jvm,
            targetName,
            {
                object : HasCompilerOptions<KotlinJvmCompilerOptions> {
                    override val options: KotlinJvmCompilerOptions =
                        project.objects.newInstance(KotlinJvmCompilerOptionsDefault::class.java)
                }
            },
            { compilerOptions: KotlinJvmCompilerOptions ->
                object : KotlinJvmOptions {
                    override val options: KotlinJvmCompilerOptions get() = compilerOptions
                }
            }
        ) as KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions>)
            .apply {
                disambiguationClassifier = null // don't add anything to the task names
            }

        (project.kotlinExtension as KotlinJvmProjectExtension).target = target

        super.apply(project)

        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)
    }

    override fun configureClassInspectionForIC(project: Project) {
        // For new IC this task is not needed
        if (!project.kotlinPropertiesProvider.useClasspathSnapshot) {
            super.configureClassInspectionForIC(project)
        }
    }
}

internal open class KotlinCommonPlugin(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    companion object {
        private const val targetName = "common"
    }

    override fun buildSourceSetProcessor(
        project: Project,
        compilation: AbstractKotlinCompilation<*>
    ): KotlinSourceSetProcessor<*> =
        KotlinCommonSourceSetProcessor(compilation, tasksProvider)

    override fun apply(project: Project) {
        @Suppress("UNCHECKED_CAST")
        val target = project.objects.newInstance(
            KotlinWithJavaTarget::class.java,
            project,
            KotlinPlatformType.common,
            targetName,
            {
                object : HasCompilerOptions<KotlinMultiplatformCommonCompilerOptions> {
                    override val options: KotlinMultiplatformCommonCompilerOptions =
                        project.objects.newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java)
                }
            },
            { compilerOptions: KotlinMultiplatformCommonCompilerOptions ->
                object : KotlinMultiplatformCommonOptions {
                    override val options: KotlinMultiplatformCommonCompilerOptions
                        get() = compilerOptions
                }
            }
        ) as KotlinWithJavaTarget<KotlinMultiplatformCommonOptions, KotlinMultiplatformCommonCompilerOptions>
        (project.kotlinExtension as KotlinCommonProjectExtension).target = target

        super.apply(project)
    }
}

@Deprecated(
    message = "Should be removed with Js platform plugin",
    level = DeprecationLevel.ERROR
)
internal open class Kotlin2JsPlugin(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    companion object {
        private const val targetName = "2Js"
    }

    override fun buildSourceSetProcessor(
        project: Project,
        compilation: AbstractKotlinCompilation<*>
    ): KotlinSourceSetProcessor<*> =
        Kotlin2JsSourceSetProcessor(tasksProvider, compilation)

    override fun apply(project: Project) {
        @Suppress("UNCHECKED_CAST")
        val target = project.objects.newInstance(
            KotlinWithJavaTarget::class.java,
            project,
            KotlinPlatformType.js,
            targetName,
            {
                object : HasCompilerOptions<KotlinJsCompilerOptions> {
                    override val options: KotlinJsCompilerOptions =
                        project.objects.newInstance(KotlinJsCompilerOptionsDefault::class.java)
                }
            },
            { compilerOptions: KotlinJsCompilerOptions ->
                object : KotlinJsOptions {
                    override val options: KotlinJsCompilerOptions
                        get() = compilerOptions
                }
            }
        ) as KotlinWithJavaTarget<KotlinJsOptions, KotlinJsCompilerOptions>

        (project.kotlinExtension as Kotlin2JsProjectExtension).setTarget(target)
        super.apply(project)
    }
}

internal open class KotlinAndroidPlugin(
    private val registry: ToolingModelBuilderRegistry
) : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleCompatibility()

        project.dynamicallyApplyWhenAndroidPluginIsApplied(
            {
                project.objects.newInstance(
                    KotlinAndroidTarget::class.java,
                    "",
                    project
                ).also {
                    (project.kotlinExtension as KotlinAndroidProjectExtension).target = it
                }
            }
        ) { androidTarget ->
            applyUserDefinedAttributes(androidTarget)
            customizeKotlinDependencies(project)
            registry.register(KotlinModelBuilder(project.getKotlinPluginVersion(), androidTarget))
            project.whenEvaluated { project.components.addAll(androidTarget.components) }
        }
    }

    companion object {
        private val minimalSupportedAgpVersion = AndroidGradlePluginVersion(4, 1, 3)
        fun androidTargetHandler(): AndroidProjectHandler {
            val tasksProvider = KotlinTasksProvider()
            val androidGradlePluginVersion = AndroidGradlePluginVersion.currentOrNull

            if (androidGradlePluginVersion != null) {
                if (androidGradlePluginVersion < minimalSupportedAgpVersion) {
                    throw IllegalStateException(
                        "Kotlin: Unsupported version of com.android.tools.build:gradle plugin: " +
                                "version $minimalSupportedAgpVersion or higher should be used with kotlin-android plugin"
                    )
                }
            }

            return AndroidProjectHandler(KotlinConfigurationTools(tasksProvider))
        }

        internal fun Project.dynamicallyApplyWhenAndroidPluginIsApplied(
            kotlinAndroidTargetProvider: () -> KotlinAndroidTarget,
            additionalConfiguration: (KotlinAndroidTarget) -> Unit = {}
        ) {
            var wasConfigured = false

            androidPluginIds.forEach { pluginId ->
                plugins.withId(pluginId) {
                    wasConfigured = true
                    val target = kotlinAndroidTargetProvider()
                    androidTargetHandler().configureTarget(target)
                    additionalConfiguration(target)
                }
            }

            afterEvaluate {
                if (!wasConfigured) {
                    throw GradleException(
                        """
                        |'kotlin-android' plugin requires one of the Android Gradle plugins.
                        |Please apply one of the following plugins to '${project.path}' project:
                        |${androidPluginIds.joinToString(prefix = "- ", separator = "\n\t- ")}
                        """.trimMargin()
                    )
                }
            }
        }
    }
}

class KotlinConfigurationTools internal constructor(
    @Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_WARNING", "EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR")
    val kotlinTasksProvider: KotlinTasksProvider
)
