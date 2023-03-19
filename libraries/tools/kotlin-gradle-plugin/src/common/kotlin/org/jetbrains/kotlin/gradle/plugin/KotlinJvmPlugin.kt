/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.*
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.*

const val PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerPluginClasspath"
const val NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinNativeCompilerPluginClasspath"
internal const val COMPILER_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerClasspath"
internal const val KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME = "kotlinKlibCommonizerClasspath"

const val KOTLIN_DSL_NAME = "kotlin"

@Deprecated("Should be removed with 'platform.js' plugin removal")
const val KOTLIN_JS_DSL_NAME = "kotlin2js"
const val KOTLIN_OPTIONS_DSL_NAME = "kotlinOptions"

internal open class KotlinJvmPlugin(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    internal companion object {
        private const val targetName = "" // use empty suffix for the task names

        internal fun Project.configureCompilerOptionsForTarget(
            extensionCompilerOptions: KotlinJvmCompilerOptions,
            @Suppress("DEPRECATION") compilationsContainer: NamedDomainObjectContainer<out AbstractKotlinCompilation<KotlinJvmOptions>>
        ) {
            extensionCompilerOptions.verbose.convention(logger.isDebugEnabled)
            compilationsContainer.configureEach {
                val jvmCompilerOptions = it.compilerOptions.options as KotlinJvmCompilerOptions
                KotlinJvmCompilerOptionsHelper.syncOptionsAsConvention(
                    from = extensionCompilerOptions,
                    into = jvmCompilerOptions
                )
                jvmCompilerOptions.moduleName.convention(
                    extensionCompilerOptions.moduleName.orElse(
                        @Suppress("DEPRECATION")
                        project.providers.provider { it.moduleName }
                    )
                )
            }
        }
    }

    override fun buildSourceSetProcessor(project: Project, compilation: KotlinCompilation<*>) =
        Kotlin2JvmSourceSetProcessor(tasksProvider, KotlinCompilationInfo(compilation))

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

        val kotlinExtension = project.kotlinExtension as KotlinJvmProjectExtension
        kotlinExtension.target = target
        project.configureCompilerOptionsForTarget(
            kotlinExtension.compilerOptions,
            target.compilations
        )

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
