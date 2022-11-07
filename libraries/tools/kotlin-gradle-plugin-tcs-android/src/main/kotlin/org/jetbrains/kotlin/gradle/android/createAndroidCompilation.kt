/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import com.android.build.gradle.internal.publishing.AndroidArtifacts
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.DecoratedKotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation

internal fun PrototypeAndroidTarget.createAndroidCompilation(name: String): PrototypeAndroidCompilation {
    return createCompilation {
        compilationName = name
        defaultSourceSet = kotlin.sourceSets.maybeCreate(camelCase("prototype", targetName, name))
        decoratedKotlinCompilationFactory = DecoratedKotlinCompilationFactory(::PrototypeAndroidCompilation)
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
                attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
                attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named(TargetJvmEnvironment.ANDROID))
            }

            /* Setup attributes for the runtime dependencies */
            compilation.configurations.runtimeDependencyConfiguration?.apply {
                attributes.attribute(AndroidArtifacts.ARTIFACT_TYPE, AndroidArtifacts.ArtifactType.CLASSES_JAR.type)
                attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, project.objects.named(TargetJvmEnvironment.ANDROID))
            }
        }
    }
}
