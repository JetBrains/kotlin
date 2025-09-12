/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.JAR_TYPE
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.isMetadataJar
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.notMetadataJar
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateDecompressed
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibViewAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.UKLIB_API_ELEMENTS_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.registerKlibArtifact

internal val KotlinJvmJarArtifact = KotlinTargetArtifact { target, apiElements, runtimeElements ->
    if (target !is KotlinJvmTarget) return@KotlinTargetArtifact
    val mainCompilation = target.compilations.getByName(MAIN_COMPILATION_NAME)

    val jvmJarTask = target.createArtifactsTask { jar ->
        jar.from(mainCompilation.output.allOutputs)
    }

    val artifact = target.createPublishArtifact(jvmJarTask, JAR_TYPE, apiElements, runtimeElements)
    val attribute = target.uklibFragmentPlatformAttribute.convertToStringForPublicationInUmanifest()
    mainCompilation.project.configurations.maybeCreateConsumable(UKLIB_API_ELEMENTS_NAME).outgoing.variants {
        it.create(attribute) {
            it.artifact(artifact)
            it.attributes {
                it.attribute(uklibStateAttribute, uklibStateDecompressed)
                it.attribute(uklibViewAttribute, attribute)
                it.attribute(isMetadataJar, notMetadataJar)
            }
        }
    }
}
