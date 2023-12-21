/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ARTIFACT_TYPE
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.android.AndroidKotlinSourceSet.Companion.android
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.CompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation


internal fun PrototypeAndroidTarget.createAndroidCompilation(name: String): PrototypeAndroidCompilation {
    return createCompilation {
        compilationName = name
        defaultSourceSet = kotlin.sourceSets.maybeCreate(camelCase("prototype", targetName, name)).apply {
            android = AndroidKotlinSourceSet()
        }
        compilationFactory = CompilationFactory(::PrototypeAndroidCompilation)
        compileTaskName = camelCase("prototype", "compile", targetName, name)

        /*
        Replace Kotlin's compilation association (main <-> test) with noop,
        since Android goes through adding a dependency on the project itself
         */
        compilationAssociator = ExternalKotlinCompilationDescriptor.CompilationAssociator { first, second ->
            first.configurations.compileDependencyConfiguration.extendsFrom(
                second.configurations.apiConfiguration,
                second.configurations.implementationConfiguration,
                second.configurations.compileOnlyConfiguration
            )
        }

        /* Configure the compilation before it is accessible for user code */
        configure { compilation ->
            /* Setup attributes for the compile dependencies */
            compilation.configurations.compileDependencyConfiguration.apply {
                attributes.attributeProvider(
                    ARTIFACT_TYPE,
                    project.provider { AndroidArtifacts.ArtifactType.CLASSES_JAR.type }
                )
                attributes.attributeProvider(
                    TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    project.provider { project.objects.named(TargetJvmEnvironment.ANDROID) }
                )
            }

            /* Setup attributes for the runtime dependencies */
            compilation.configurations.runtimeDependencyConfiguration?.apply {
                attributes.attributeProvider(
                    ARTIFACT_TYPE,
                    project.provider { AndroidArtifacts.ArtifactType.CLASSES_JAR.type }
                )
                attributes.attributeProvider(
                    TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    project.provider { project.objects.named(TargetJvmEnvironment.ANDROID) }
                )
            }
        }
    }
}
