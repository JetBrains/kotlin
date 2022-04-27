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
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.createExternalJvmVariant
import org.jetbrains.kotlin.gradle.kpm.external.external
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import java.util.*

fun KotlinGradleModule.createKotlinAndroidVariant(androidVariant: BaseVariant) {
    val androidOutgoingArtifacts = FragmentArtifacts<KotlinJvmVariant> {
        variants.create("classes") { variant ->
            variant.attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
            variant.artifact(project.provider { fragment.compilationOutputs.classesDirs.singleFile }) {
                it.builtBy(fragment.compilationOutputs.classesDirs)
            }
        }

        if (androidVariant is LibraryVariant) {
            variants.create("aar") { variant ->
                variant.attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.AAR.type)
                variant.artifact(androidVariant.packageLibraryProvider)
            }
        }
    }

    val androidElementsAttributes = FragmentAttributes<KotlinJvmVariant> {
        attribute(BuildTypeAttr.ATTRIBUTE, project.objects.named(androidVariant.buildType.name))
        attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named(TargetJvmEnvironment.ANDROID))
    }

    val kotlinVariant = createExternalJvmVariant(
        "android${androidVariant.buildType.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }}", KotlinJvmVariantConfig(
            /* Only swap out configuration that is used. Default setup shall still be applied */
            compileDependencies = (DefaultKotlinCompileDependenciesDefinition +
                    FragmentAttributes<KotlinGradleFragment> {
                        namedAttribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE, TargetJvmEnvironment.ANDROID)
                        attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                    })
                .withConfigurationProvider { androidVariant.compileConfiguration },

            /* Only swap out configuration that is used. Default setup shall still be applied */
            runtimeDependencies = DefaultKotlinRuntimeDependenciesDefinition
                .withConfigurationProvider { androidVariant.runtimeConfiguration },

            /* Add android artifacts and attributes */
            apiElements = DefaultKotlinApiElementsDefinition + androidElementsAttributes + androidOutgoingArtifacts,

            /* Add android artifacts and attributes */
            runtimeElements = DefaultKotlinRuntimeElementsDefinition + androidElementsAttributes + androidOutgoingArtifacts,

            /* For now: Just publish 'release' (non-debuggable) variants */
            publicationConfigurator = if (androidVariant.buildType.isDebuggable) KotlinPublicationConfigurator.NoPublication else
                KotlinPublicationConfigurator.SingleVariantPublication
        )
    )

    // "Disable" configurations from plain Android plugin
    project.configurations.findByName("${androidVariant.name}ApiElements")?.isCanBeConsumed = false
    project.configurations.findByName("${androidVariant.name}RuntimeElements")?.isCanBeConsumed = false

    // TODO: Move this into configurator!
    kotlinVariant.refines(androidCommon)

    val mainBytecodeKey = androidVariant.registerPreJavacGeneratedBytecode(
        kotlinVariant.compilationOutputs.classesDirs
    )

    kotlinVariant.compileDependencyFiles = project.files(
        androidVariant.getCompileClasspath(mainBytecodeKey),
        project.getAndroidRuntimeJars()
    )

    val androidDsl = AndroidDsl()
    androidDsl.androidManifest = project.file("AndroidManifest.xml")
    androidDsl.compileSdk = 23
    androidCommon.external[androidDslKey] = androidDsl
    kotlinVariant.external[androidDslKey] = androidDsl
}
