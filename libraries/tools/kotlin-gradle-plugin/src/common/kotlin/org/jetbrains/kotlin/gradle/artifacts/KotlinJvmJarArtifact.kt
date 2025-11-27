/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.kotlinMultiplatformRootPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateDecompressed
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibViewAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.maybeCreateUklibApiElements
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.maybeCreateUklibRuntimeElements
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.utils.registerArtifact
import org.jetbrains.kotlin.gradle.utils.registerKlibArtifact

internal val KotlinJvmJarArtifact = KotlinTargetArtifact { target, apiElements, runtimeElements ->
    if (target !is KotlinJvmTarget) return@KotlinTargetArtifact
    val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)

    val jvmJarTask = target.createArtifactsTask { jar ->
        jar.from(mainCompilation.output.allOutputs)
    }

    val artifact = target.createPublishArtifact(jvmJarTask, JAR_TYPE, apiElements, runtimeElements)

    when (target.project.kotlinPropertiesProvider.kmpPublicationStrategy) {
        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> {
            val uklibAttribute = target.uklibFragmentPlatformAttribute.convertToStringForPublicationInUmanifest()
            listOf(
                mainCompilation.project.maybeCreateUklibApiElements(),
                mainCompilation.project.maybeCreateUklibRuntimeElements(),
            ).forEach {
                it.outgoing.variants {
                    val variant = it.maybeCreate(uklibAttribute)
                    // FIXME: Use .jar for JPMS support in interproject UKlib. Review this in KT-81375
                    variant.artifacts.add(artifact)
                    variant.attributes {
                        it.attribute(uklibStateAttribute, uklibStateDecompressed)
                        it.attribute(uklibViewAttribute, uklibAttribute)
                    }
                }
            }

            target.project.launch {
                val rootPublication = target.project.kotlinMultiplatformRootPublication.await()
                rootPublication?.artifact(artifact)
            }
        }
        KmpPublicationStrategy.StandardKMPPublication -> {}
    }
}
