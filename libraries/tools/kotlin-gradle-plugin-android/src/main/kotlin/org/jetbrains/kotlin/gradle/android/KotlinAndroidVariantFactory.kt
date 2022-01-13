/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.android

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalJvmVariant
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration.DefaultKotlinApiElementsConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration.DefaultKotlinRuntimeElementsConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration.KotlinCompileDependenciesConfigurationInstantiator
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration.KotlinRuntimeDependenciesConfigurationInstantiator
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.KotlinNameDisambiguation

fun KotlinGradleModule.createKotlinAndroidVariant(androidVariant: BaseVariant) {
    val androidElementsConfigurator = KotlinFragmentConfigurationsConfigurator<KotlinJvmVariant> { fragment, configuration ->
        // TODO NOW: Avoid this in publication -> we have to use a different `-published` configuration?
        configuration.attributes.attribute(BuildTypeAttr.ATTRIBUTE, project.objects.named(androidVariant.buildType.name))
        configuration.attributes.attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named(TargetJvmEnvironment.ANDROID))

        configuration.outgoing.variants { variants ->
            if (androidVariant is LibraryVariant) {
                variants.create("aar") { variant ->
                    variant.attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.AAR.type)
                    variant.artifact(androidVariant.packageLibraryProvider)
                }
            }

            variants.create("classes") { variant ->
                variant.attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
                variant.artifact(project.provider { fragment.compilationOutputs.classesDirs.singleFile }) {
                    it.builtBy(fragment.compilationOutputs.classesDirs)
                }
            }
        }
    }

    val kotlinVariant = createExternalJvmVariant(
        "android${androidVariant.buildType.name.capitalize()}",
        instantiator = KotlinJvmVariantInstantiator(
            this,
            compileDependenciesConfigurationInstantiator = object : KotlinCompileDependenciesConfigurationInstantiator {
                override fun create(
                    module: KotlinGradleModule, names: KotlinNameDisambiguation, dependencies: KotlinFragmentDependencyConfigurations
                ): Configuration = androidVariant.compileConfiguration.apply {
                    extendsFrom(dependencies.transitiveApiConfiguration)
                    extendsFrom(dependencies.transitiveImplementationConfiguration)
                }
            },
            runtimeDependenciesConfigurationInstantiator = object : KotlinRuntimeDependenciesConfigurationInstantiator {
                override fun create(
                    module: KotlinGradleModule, names: KotlinNameDisambiguation, dependencies: KotlinFragmentDependencyConfigurations
                ): Configuration = androidVariant.runtimeConfiguration.apply {
                    extendsFrom(dependencies.transitiveApiConfiguration)
                    extendsFrom(dependencies.transitiveImplementationConfiguration)
                }
            },
            /*
            apiElementsConfigurationInstantiator = object : KotlinApiElementsConfigurationInstantiator {
                override fun create(
                    module: KotlinGradleModule, names: KotlinNameDisambiguation, dependencies: KotlinFragmentDependencyConfigurations
                ): Configuration {
                    return module.project.configurations.findByName("${androidVariant.name}ApiElements")
                    // TODO NOW: Thought: Maybe I do not want any elements?
                        ?: DefaultKotlinApiElementsConfigurationInstantiator.create(module, names, dependencies)
                }
            },
            runtimeElementsConfigurationInstantiator = object : KotlinRuntimeElementsConfigurationInstantiator {
                override fun create(
                    module: KotlinGradleModule, names: KotlinNameDisambiguation, dependencies: KotlinFragmentDependencyConfigurations
                ): Configuration {
                    return module.project.configurations.findByName("${androidVariant.name}RuntimeElements")
                        ?: DefaultKotlinRuntimeElementsConfigurationInstantiator.create(module, names, dependencies)
                }
            }
             */
        ),

        configurator = KotlinJvmVariantConfigurator(
            // TODO NOW: Publication!
            publicationConfigurator = if (androidVariant.buildType.isDebuggable) KotlinPublicationConfigurator.NoPublication else
                KotlinPublicationConfigurator.SingleVariantPublication,

            // TODO NOW: Thought: Maybe configuration and instantiation of such elements should be somehow done together?
            apiElementsConfigurator = DefaultKotlinApiElementsConfigurator + androidElementsConfigurator,
            /*{ fragment, configuration ->
                KotlinFragmentPlatformAttributesConfigurator.configure(fragment, configuration)
                KotlinFragmentProducerApiUsageAttributesConfigurator.configure(fragment, configuration)
            }*/
            runtimeElementsConfigurator = DefaultKotlinRuntimeElementsConfigurator + androidElementsConfigurator, /*{ fragment, configuration ->
                KotlinFragmentPlatformAttributesConfigurator.configure(fragment, configuration)
                KotlinFragmentProducerRuntimeUsageAttributesConfigurator.configure(fragment, configuration)
            } */
        )
    )

    // "Disable" configurations from plain Android plugin
    project.configurations.findByName("${androidVariant.name}ApiElements")?.isCanBeConsumed = false
    project.configurations.findByName("${androidVariant.name}RuntimeElements")?.isCanBeConsumed = false

    // TODO: Move this into configurator!
    kotlinVariant.refines(androidCommon)

    /*
    androidVariant.compileConfiguration.apply {
        extendsFrom(kotlinVariant.compileDependenciesConfiguration)
    }

    androidVariant.runtimeConfiguration.apply {
        extendsFrom(kotlinVariant.runtimeDependenciesConfiguration)
    }
     */

    val mainBytecodeKey = androidVariant.registerPreJavacGeneratedBytecode(
        kotlinVariant.compilationOutputs.classesDirs
    )

    kotlinVariant.compileDependencyFiles = project.files(
        androidVariant.getCompileClasspath(mainBytecodeKey),
        project.getAndroidRuntimeJars()
    )
}

