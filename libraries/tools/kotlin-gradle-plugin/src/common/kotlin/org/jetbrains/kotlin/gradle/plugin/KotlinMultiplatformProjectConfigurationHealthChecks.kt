/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.android.kotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.plugin.sources.android.multiplatformAndroidSourceSetLayoutV2
import org.jetbrains.kotlin.gradle.targets.android.findAndroidTarget
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerDependent
import org.jetbrains.kotlin.gradle.targets.native.internal.from
import org.jetbrains.kotlin.gradle.targets.native.internal.isAllowCommonizer
import org.jetbrains.kotlin.gradle.utils.findAppliedAndroidPluginIdOrNull
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.getOrPut
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheck
import org.jetbrains.kotlin.tooling.core.withLinearClosure

private class KotlinMultiplatformProjectConfigurationException(message: String) : Exception(message)

internal fun Project.runMissingKotlinTargetsProjectConfigurationHealthCheck() = project.runProjectConfigurationHealthCheck {
    val isNoTargetsInitialized = (project.kotlinExtension as KotlinMultiplatformExtension)
        .targets
        .none { it !is KotlinMetadataTarget }

    if (isNoTargetsInitialized) {
        throw KotlinMultiplatformProjectConfigurationException(
            """
                Please initialize at least one Kotlin target in '${project.name} (${project.path})'.
                Read more https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets
                """.trimIndent()
        )
    }
}

internal fun Project.runMissingAndroidTargetProjectConfigurationHealthCheck(
    warningLogger: (warningMessage: String) -> Unit = project.logger::warn
) = project.runProjectConfigurationHealthCheck check@{
    if (project.kotlinPropertiesProvider.ignoreAbsentAndroidMultiplatformTarget) {
        return@check
    }

    val androidPluginId = findAppliedAndroidPluginIdOrNull() ?: return@check

    if (findAndroidTarget() != null) return@check

    warningLogger(
        """
            Missing 'androidTarget()' Kotlin target in multiplatform project ${project.name} (${project.path})'.
            The Android Gradle plugin was applied without creating a corresponding 'androidTarget()' Kotlin Target:
            
            ```
            plugins {
                id("$androidPluginId")
                kotlin("multiplatform")
            }
            
            kotlin {
                androidTarget() // <-- please register this Android target
            }
            ```
          
        """.trimIndent()
    )
}

internal fun Project.runDisabledCInteropCommonizationOnHmppProjectConfigurationHealthCheck(
    warningLogger: (warningMessage: String) -> Unit = project.logger::warn
) {
    if (kotlinPropertiesProvider.ignoreDisabledCInteropCommonization) return
    if (isAllowCommonizer() && !kotlinPropertiesProvider.enableCInteropCommonization) {
        val multiplatformExtension = multiplatformExtensionOrNull ?: return

        val sharedCompilationsWithInterops = multiplatformExtension.targets.flatMap { it.compilations }
            .filterIsInstance<KotlinSharedNativeCompilation>()
            .mapNotNull { compilation ->
                val cinteropDependent = future { CInteropCommonizerDependent.from(compilation) }.getOrThrow() ?: return@mapNotNull null
                compilation to cinteropDependent
            }
            .toMap()

        val affectedCompilations = sharedCompilationsWithInterops.keys
        val affectedCInterops = sharedCompilationsWithInterops.values.flatMap { it.interops }.toSet()


        /* CInterop commonizer would not affect the project: No compilation that would actually benefit */
        if (affectedCompilations.isEmpty()) return
        if (affectedCInterops.isEmpty()) return

        warningLogger(
            """
                [WARNING] The project is using Kotlin Multiplatform with hierarchical structure and disabled 'cinterop commonization'
                    See: https://kotlinlang.org/docs/mpp-share-on-platforms.html#use-native-libraries-in-the-hierarchical-structure
               
                    'cinterop commonization' can be enabled in your 'gradle.properties'
                    kotlin.mpp.enableCInteropCommonization=true
                    
                    To hide this message, add to your 'gradle.properties'
                    $KOTLIN_MPP_ENABLE_CINTEROP_COMMONIZATION.nowarn=true 
                
                    The following source sets are affected: 
                    ${affectedCompilations.map { it.defaultSourceSetName }.sorted().joinToString(", ", "[", "]")}
                    
                    The following cinterops are affected: 
                    ${affectedCInterops.map { it.toString() }.sorted().joinToString(", ", "[", "]")}
                    
            """.trimIndent()
        )
    }
}

internal fun Project.runAndroidSourceSetLayoutV1SourceSetsNotFoundDiagnostic(
    warningLogger: (warningMessage: String) -> Unit = project.logger::warn
) = whenEvaluated check@{
    val failure = project.state.failure ?: return@check

    /* Checker is only relevant if multiplatformAndroidSourceSetLayoutV2 is applied */
    if (project.kotlinAndroidSourceSetLayout != multiplatformAndroidSourceSetLayoutV2) return@check
    val androidSourceSetRegex by lazy { Regex("""(androidTest|androidAndroidTest)\w*""") }

    val allReasons = failure.withLinearClosure { it.cause }

    val unknownAndroidSourceSetNames = allReasons.filterIsInstance<UnknownDomainObjectException>()
        .filter { it.message.orEmpty().contains("KotlinSourceSet") }
        .mapNotNull { androidSourceSetRegex.find(it.message.orEmpty()) }
        .map { it.value }
        .toSet()

    unknownAndroidSourceSetNames.forEach { unknownAndroidSourceSetName ->
        warningLogger(createAndroidSourceSetLayoutV1SourceSetsNotFoundWarning(unknownAndroidSourceSetName))
    }
}

internal fun createAndroidSourceSetLayoutV1SourceSetsNotFoundWarning(sourceSetName: String): String =
    """
    e: KotlinSourceSet with name '$sourceSetName' not found:
    The SourceSet requested ('$sourceSetName') was renamed in Kotlin 1.9.0
    
    In order to migrate you might want to replace: 
    sourceSets.getByName("androidTest") -> sourceSets.getByName("androidUnitTest")
    sourceSets.getByName("androidAndroidTest") -> sourceSets.getByName("androidInstrumentedTest")
    
    Learn more about the new Kotlin/Android SourceSet Layout: 
    https://kotlinlang.org/docs/whatsnew18.html#kotlin-multiplatform-a-new-android-source-set-layout
    """.trimIndent()
