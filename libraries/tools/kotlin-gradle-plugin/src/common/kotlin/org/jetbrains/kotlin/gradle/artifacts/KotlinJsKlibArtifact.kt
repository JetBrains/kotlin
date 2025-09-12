/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.isMetadataJar
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.notMetadataJar
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateDecompressed
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibViewAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.UKLIB_API_ELEMENTS_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.wasmDecamelizedDefaultNameOrNull
import org.jetbrains.kotlin.gradle.utils.decamelize
import org.jetbrains.kotlin.gradle.utils.libsDirectory
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable

internal val KotlinJsKlibArtifact = KotlinTargetArtifact { target, apiElements, runtimeElements ->
    if (target !is KotlinJsIrTarget) return@KotlinTargetArtifact
    val mainCompilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)

    val jsKlibTask = target.createArtifactsTask {
        it.from(mainCompilation.output.allOutputs)
        it.archiveExtension.set(KLIB_TYPE)
        it.destinationDirectory.set(target.project.libsDirectory)

        if (target.platformType == KotlinPlatformType.wasm) {
            if (target.wasmDecamelizedDefaultNameOrNull() != null) {
                target.disambiguationClassifier?.let { classifier ->
                    it.archiveAppendix.set(classifier.decamelize())
                }
            }
        }
    }

    val artifact = target.createPublishArtifact(jsKlibTask, KLIB_TYPE, apiElements, runtimeElements)
    val attribute = target.uklibFragmentPlatformAttribute.convertToStringForPublicationInUmanifest()
    target.project.configurations.maybeCreateConsumable(UKLIB_API_ELEMENTS_NAME).outgoing.variants {
        it.create(attribute) {
            it.artifact(artifact)
            it.attributes {
                it.attribute(uklibStateAttribute, uklibStateDecompressed)
                it.attribute(uklibViewAttribute, attribute)
            }
        }
    }
}
