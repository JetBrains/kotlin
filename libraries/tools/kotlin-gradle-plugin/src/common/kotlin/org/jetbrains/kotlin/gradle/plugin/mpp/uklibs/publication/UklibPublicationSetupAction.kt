/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication


import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.ERROR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.WARNING
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSoftwareComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.kotlinMultiplatformRootPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.Uklib
import org.jetbrains.kotlin.gradle.utils.createConsumable

internal val UklibPublicationSetupAction = KotlinProjectSetupAction {
    val kotlinSoftwareComponent = project.multiplatformExtension.rootSoftwareComponent
    when (project.kotlinPropertiesProvider.kmpPublicationStrategy) {
        KmpPublicationStrategy.StandardKMPPublication -> {
            kotlinSoftwareComponent.uklibUsages.complete(emptyList())
            return@KotlinProjectSetupAction
        }
        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> {}
    }

    if (!project.kotlinPropertiesProvider.enableKlibsCrossCompilation) {
        /**
         * Uklib must publish with all fragments. Make sure cross compilation is enabled, so that Apple klib compilations run
         */
        project.reportDiagnostic(
            KotlinToolingDiagnostics.UklibPublicationWithoutCrossCompilation(
                severity = WARNING
            ).get()
        )
    }

    project.registerOutgoingUklibVariants(kotlinSoftwareComponent)
    project.changeRootComponentPackaging()

    /**
     * Check that KGP model is compliant with Uklib limitations; this function does validations as a side effect
     */
    project.launch {
        project.multiplatformExtension.validateKgpModelIsUklibCompliantAndCreateKgpFragments()
    }
}

private fun Project.registerOutgoingUklibVariants(rootComponent: KotlinSoftwareComponent) {
    project.launch {
        rootComponent.uklibUsages.complete(
            project.createUklibOutgoingVariantsAndPublication()
        )
    }
}

private fun Project.changeRootComponentPackaging() {
    project.launch {
        val rootPublication = kotlinMultiplatformRootPublication.await()
        if (rootPublication != null) {
            rootPublication.pom.packaging = Uklib.UKLIB_PACKAGING
        }
    }
}