/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.internal.compatibilityConventionRegistrar
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.rewriteKmpDependenciesInPomForTargetPublication
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinSourceSetFactory
import org.jetbrains.kotlin.gradle.targets.jvm.ConfigureJavaTestFixturesSideEffect
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.jvm.configureKotlinConventions
import org.jetbrains.kotlin.gradle.targets.jvm.kotlinSourceSetDslName
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.utils.addSecondaryOutgoingJvmClassesVariant
import org.jetbrains.kotlin.gradle.utils.javaSourceSets
import org.jetbrains.kotlin.gradle.utils.whenEvaluated
import org.jetbrains.kotlin.tooling.core.extrasKeyOf

const val PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerPluginClasspath"
const val NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinNativeCompilerPluginClasspath"
const val COMPILER_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerClasspath"
internal const val BUILD_TOOLS_API_CLASSPATH_CONFIGURATION_NAME = "kotlinBuildToolsApiClasspath"
internal const val KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME = "kotlinKlibCommonizerClasspath"
internal const val KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME = "kotlinNativeBundleConfiguration"
internal const val KOTLIN_BOUNCY_CASTLE_CONFIGURATION_NAME = "kotlinBouncyCastleConfiguration"

internal abstract class AbstractKotlinPlugin(
    val tasksProvider: KotlinTasksProvider,
) : Plugin<Project> {

    internal abstract fun buildSourceSetProcessor(
        project: Project,
        compilation: KotlinCompilation<*>,
    ): KotlinSourceSetProcessor<*>

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)

        val target = (project.kotlinExtension as KotlinSingleJavaTargetExtension).target

        configureTarget(
            target,
            { compilation -> buildSourceSetProcessor(project, compilation) }
        )

        rewriteMppDependenciesInPom(target)
        project.components.addAll(target.components)
    }

    private fun rewriteMppDependenciesInPom(target: AbstractKotlinTarget) {
        val project = target.project

        project.pluginManager.withPlugin("maven-publish") {
            project.extensions.configure(PublishingExtension::class.java) { publishing ->
                publishing.publications.withType(MavenPublication::class.java).all { publication ->
                    project.rewriteKmpDependenciesInPomForTargetPublication(
                        component = target.kotlinComponents.single(),
                        publication = publication
                    )
                }
            }
        }
    }

    companion object {

        fun configureTarget(
            target: KotlinWithJavaTarget<*, *>,
            buildSourceSetProcessor: (KotlinCompilation<*>) -> KotlinSourceSetProcessor<*>,
        ) {
            setUpJavaSourceSets(target)
            configureSourceSetDefaults(target, buildSourceSetProcessor)
            configureAttributes(target)
            ConfigureJavaTestFixturesSideEffect(target)
        }

        internal fun setUpJavaSourceSets(
            kotlinTarget: KotlinTarget,
            duplicateJavaSourceSetsAsKotlinSourceSets: Boolean = true,
        ) {
            val project = kotlinTarget.project
            val javaSourceSets = project.javaSourceSets

            val kotlinSourceSetDslName = kotlinTarget.kotlinSourceSetDslName
            val isMppJvmTarget = kotlinTarget is KotlinJvmTarget
            javaSourceSets.all { javaSourceSet ->
                if (isMppJvmTarget && kotlinTarget.extras[extrasKeyOf<Boolean>(KotlinJvmCompilationFactory.EXTRA_CREATING_DEFAULT_JAVA_SOURCE_NAME)] == true) return@all
                // KotlinJvmCompilation for this SourceSet already exist, no need to proceed
                if (isMppJvmTarget && kotlinTarget.compilations.any { it.defaultSourceSet.name == javaSourceSet.name }) return@all

                val kotlinCompilation = kotlinTarget.compilations.maybeCreate(javaSourceSet.name)

                if (duplicateJavaSourceSetsAsKotlinSourceSets) {
                    project.configurations
                        .findByName(javaSourceSet.apiElementsConfigurationName)
                        ?.addSecondaryOutgoingJvmClassesVariant(project, kotlinCompilation)

                    val kotlinSourceSet = project.kotlinExtension.sourceSets.maybeCreate(kotlinCompilation.name)
                    kotlinSourceSet.kotlin.source(javaSourceSet.java)

                    // Registering resources from JavaSourceSet as KotlinSourceSet resources.
                    // In the case of KotlinPlugin Java Sources set will create ProcessResources task to process all resources into output
                    // 'kotlinSourceSet.resources' should contain Java SourceSet default resource directories,
                    // and to avoid duplication error, we are replacing the already created default one.
                    with(kotlinSourceSet as DefaultKotlinSourceSet) {
                        val defaultResources = actualResources
                        actualResources = javaSourceSet.resources
                        // Filtering out default resource directory to avoid duplicates error
                        val defaultResourcesDir = KotlinSourceSetFactory.defaultSourceFolder(
                            project,
                            javaSourceSet.name,
                            javaSourceSet.resources.name
                        )
                        resources.srcDir(defaultResources.sourceDirectories.filter {
                            !it.startsWith(defaultResourcesDir)
                        })
                    }

                    @Suppress("DEPRECATION_ERROR")
                    kotlinCompilation.addSourceSet(kotlinSourceSet)
                    project.compatibilityConventionRegistrar.addConvention(javaSourceSet, kotlinSourceSetDslName, kotlinSourceSet)
                    javaSourceSet.addExtension(kotlinSourceSetDslName, kotlinSourceSet.kotlin)
                } else {
                    javaSourceSet.configureKotlinConventions(project, kotlinCompilation)
                }
            }

            kotlinTarget.compilations.all { kotlinCompilation ->
                @Suppress("DEPRECATION_ERROR")
                kotlinCompilation.addSourceSet(kotlinCompilation.defaultSourceSet)
            }

            kotlinTarget.compilations.run {
                getByName(KotlinCompilation.TEST_COMPILATION_NAME).associateWith(getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
            }

            // Since the 'java' plugin (as opposed to 'java-library') doesn't known anything about the 'api' configurations,
            // add the API dependencies of the main compilation directly to the 'apiElements' configuration, so that the 'api' dependencies
            // are properly published with the 'compile' scope (KT-28355):
            project.whenEvaluated {
                project.configurations.apply {
                    val apiElementsConfiguration = getByName(kotlinTarget.apiElementsConfigurationName)
                    val mainCompilation = kotlinTarget.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                    val compilationApiConfiguration = getByName(mainCompilation.legacyApiConfigurationName)
                    apiElementsConfiguration.extendsFrom(compilationApiConfiguration)
                }
            }
        }

        private fun configureAttributes(
            kotlinTarget: KotlinWithJavaTarget<*, *>,
        ) {
            val project = kotlinTarget.project

            // Setup the consuming configurations:
            project.dependencies.attributesSchema.attribute(KotlinPlatformType.attribute)

            // Setup the published configurations:
            // Don't set the attributes for common module; otherwise their 'common' platform won't be compatible with the one in
            // platform-specific modules
            if (kotlinTarget.platformType != KotlinPlatformType.common) {
                project.configurations.getByName(kotlinTarget.apiElementsConfigurationName).run {
                    KotlinUsages.configureProducerApiUsage(this, kotlinTarget)
                    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                    usesPlatformOf(kotlinTarget)
                }

                project.configurations.getByName(kotlinTarget.runtimeElementsConfigurationName).run {
                    KotlinUsages.configureProducerRuntimeUsage(this, kotlinTarget)
                    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                    usesPlatformOf(kotlinTarget)
                }
            }
        }

        private fun configureSourceSetDefaults(
            kotlinTarget: KotlinWithJavaTarget<*, *>,
            buildSourceSetProcessor: (KotlinCompilation<*>) -> KotlinSourceSetProcessor<*>,
        ) {
            kotlinTarget.compilations.all { compilation ->
                buildSourceSetProcessor(compilation).run()
            }
        }
    }
}
