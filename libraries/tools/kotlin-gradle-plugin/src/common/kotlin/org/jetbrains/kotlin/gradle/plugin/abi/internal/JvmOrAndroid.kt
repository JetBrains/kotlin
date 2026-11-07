/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.abi.AbiValidationExtension
import org.jetbrains.kotlin.gradle.dsl.abi.BinariesSource
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic

/**
 * Finalizes the configuration of the report variant for the JVM version of the Kotlin Gradle plugin.
 */
internal fun AbiValidationExtension.finalizeJvmVariant(
    project: Project,
    target: KotlinTarget,
    abiConfiguration: AbiValidationConfiguration,
) {
    finalizeVariant(project, binariesSource.get(), MAIN_COMPILATION_NAME, target, abiConfiguration)
}


/**
 * Finalizes the configuration of the report variant for the Android version of the Kotlin Gradle plugin.
 */
internal fun AbiValidationExtension.finalizeAndroidVariant(
    project: Project,
    target: KotlinTarget,
    abiConfiguration: AbiValidationConfiguration,
) {
    finalizeVariant(project, binariesSource.get(), ANDROID_RELEASE_BUILD_TYPE, target, abiConfiguration)
}

private fun finalizeVariant(
    project: Project,
    binariesSource: BinariesSource,
    compilationName: String,
    target: KotlinTarget,
    abiConfiguration: AbiValidationConfiguration
) {
    val classfiles = project.files()
    abiConfiguration.addJvmDump(classfiles)

    when (binariesSource) {
        BinariesSource.MAVEN_PUBLICATIONS -> {
            project.analyzeMavenPublicationForJvm(classfiles)
        }
        BinariesSource.MAIN_COMPILATION -> {
            target.compilations.withCompilationIfExists(compilationName) {
                classfiles.from(output.classesDirs)
            }
        }
        BinariesSource.NON_TEST_COMPILATIONS -> {
            target.compilations.configureEach { compilation ->
                if (!compilation.compilationName.contains("test", ignoreCase = true)) {
                    classfiles.from(compilation.output.classesDirs)
                }
            }
        }
    }
}

internal fun Project.analyzeMavenPublicationForJvm(classfiles: ConfigurableFileCollection) {
    val publishingExtension = extensions.findByType(PublishingExtension::class.java)
    if (publishingExtension == null) {
        reportDiagnostic(KotlinToolingDiagnostics.AbiValidationNoPublishPlugin())
    }

    publishingExtension.publications.configureEach { publication ->
        if (publication is MavenPublication) {
            publication.artifacts.configureEach { artifact ->
                if (artifact.classifier == null) {
                    classfiles.from(
                        files(artifact.file).builtBy(artifact.buildDependencies)
                    )
                }
            }
        }
    }
}
