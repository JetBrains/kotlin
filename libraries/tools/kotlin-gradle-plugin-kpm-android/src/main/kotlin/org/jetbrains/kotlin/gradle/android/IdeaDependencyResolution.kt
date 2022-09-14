/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.internal.publishing.AndroidArtifacts
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.project
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmDependency.Companion.CLASSPATH_BINARY_TYPE
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProjectModelBuilder.FragmentConstraint
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmResolvedBinaryDependencyImpl
import org.jetbrains.kotlin.gradle.kpm.idea.configureIdeaKpmSpecialPlatformDependencyResolution
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinPm20ProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.containingVariants

val isAndroidFragment = FragmentConstraint { fragment ->
    fragment.containingVariants.all { variant -> variant.androidDsl != null }
}

val isAndroidAndJvmSharedFragment = FragmentConstraint constraint@{ fragment ->
    val variants = fragment.containingVariants
    if (variants.any { it.platformType != KotlinPlatformType.jvm }) return@constraint false
    variants.any { it.androidDsl != null } && variants.any { it.androidDsl == null }
}

@OptIn(ExternalVariantApi::class, InternalKotlinGradlePluginApi::class)
internal fun KotlinPm20ProjectExtension.setupIdeaKpmFragmentDependencyResolver() {
    configureIdeaKpmSpecialPlatformDependencyResolution {

        /*
        Handle android + jvm use cases:
        We do not yet support jvm based metadata compilations, therefore we do not
        expect any reasonable results coming from metadata resolution.
        We default to a 'KotlinPlatformType.jvm' resolution
         */
        withConstraint(isAndroidAndJvmSharedFragment) {
            withPlatformResolutionAttributes {
                namedAttribute(Usage.USAGE_ATTRIBUTE, Usage.JAVA_API)
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
            }

            artifactView(CLASSPATH_BINARY_TYPE)
        }

        withConstraint(isAndroidFragment) {
            withPlatformResolutionAttributes {
                namedAttribute(Usage.USAGE_ATTRIBUTE, Usage.JAVA_API)
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
            }

            artifactView(CLASSPATH_BINARY_TYPE) {
                attributes {
                    attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
                }
            }

            artifactView("manifest") {
                attributes {
                    attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.MANIFEST.type)
                }
            }

            artifactView("resources") {
                attributes {
                    attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.ANDROID_RES.type)
                }
            }

            artifactView("android-symbol") {
                attributes {
                    attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.COMPILE_SYMBOL_LIST.type)
                }
            }

            additionalDependencies {
                project.getAndroidRuntimeJars().map { androidRuntimeJar ->
                    IdeaKpmResolvedBinaryDependencyImpl(
                        binaryType = CLASSPATH_BINARY_TYPE,
                        binaryFile = androidRuntimeJar,
                        coordinates = null
                    )
                }
            }
        }
    }
}
