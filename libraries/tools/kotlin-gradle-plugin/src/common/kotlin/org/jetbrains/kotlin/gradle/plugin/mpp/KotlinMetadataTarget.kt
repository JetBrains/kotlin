/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptionsDefault
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterFinaliseCompilations
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.sources.defaultImpl
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.newInstance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import javax.inject.Inject

abstract class KotlinMetadataTarget @Inject constructor(
    project: Project,
) : KotlinOnlyTarget<KotlinCompilation<Any>>(project, KotlinPlatformType.common),
    HasConfigurableKotlinCompilerOptions<KotlinCommonCompilerOptions> {

    override val artifactsTaskName: String
        // The IDE import looks at this task name to determine the artifact and register the path to the artifact;
        // in HMPP, since the project resolves to the all-metadata JAR, the IDE import needs to work with that JAR, too
        get() = KotlinMetadataTargetConfigurator.ALL_METADATA_JAR_NAME

    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        val mainCompilation = compilations.getByName(MAIN_COMPILATION_NAME)
        val usageContexts = buildSet {
            add(
                DefaultKotlinUsageContextMaybeReplacedWithKar(
                    compilation = mainCompilation,
                    mavenScope = KotlinUsageContext.MavenScope.COMPILE,
                    dependencyConfigurationName = apiElementsConfigurationName,
                )
            )

            val sourcesElements = sourcesElementsConfigurationName
            if (isSourcesPublishable) {
                addSourcesJarArtifactToConfiguration(
                    sourcesElements,
                    classifierPrefix = when (project.kotlinPropertiesProvider.kmpPublicationStrategy) {
                        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> "metadata"
                        KmpPublicationStrategy.StandardKMPPublication -> null
                    },
                )
                add(
                    DefaultKotlinUsageContext(
                        compilation = mainCompilation,
                        dependencyConfigurationName = sourcesElements,
                        includeIntoProjectStructureMetadata = false,
                        publishOnlyIf = { isSourcesPublishable }
                    )
                )
            }
        }

        val result = createKotlinVariant(componentName = null, usageContexts)

        setOf(result)
    }

    private fun addSourcesJarArtifactToConfiguration(
        configurationName: String,
        classifierPrefix: String?,
    ): PublishArtifact {
        return project.artifacts.add(configurationName, sourcesJarTask) { sourcesJarArtifact ->
            sourcesJarArtifact.classifier = dashSeparatedName(
                listOfNotNull(
                    classifierPrefix,
                    "sources",
                )
            )
        }
    }

    /**
     * Registration (during object init) of [sourcesJarTask] is required for cases when
     * user build scripts want to have access to sourcesJar task to configure it
     */
    private val sourcesJarTask: TaskProvider<Jar> by lazy {
        sourcesJarTaskNamed(
            taskName = "sourcesJar",
            componentName = name,
            project = project,
            sourceSets = project.future {
                allPublishableSourceSets().associate { it.name to it.defaultImpl.allKotlin }
            },
            artifactNameAppendix = name.toLowerCaseAsciiOnly()
        )
    }

    private suspend fun allPublishableSourceSets(): Set<KotlinSourceSet> {
        AfterFinaliseCompilations.await()
        return project.multiplatformExtensionOrNull?.awaitTargets().orEmpty().flatMap { target ->
            target.compilations.findByName(MAIN_COMPILATION_NAME)?.allKotlinSourceSets.orEmpty()
        }.toSet()
    }


    override val compilerOptions: KotlinCommonCompilerOptions = project.objects
        .newInstance<KotlinCommonCompilerOptionsDefault>()

    companion object {
        const val METADATA_TARGET_NAME = "metadata"
    }
}
