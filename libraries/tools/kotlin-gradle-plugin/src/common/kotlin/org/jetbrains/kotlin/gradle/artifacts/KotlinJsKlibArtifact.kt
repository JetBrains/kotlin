/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.tasks.ProducesKlib
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibStateDecompressed
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.consumption.uklibViewAttribute
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.KmpPublicationStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.UKLIB_API_ELEMENTS_NAME
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.maybeCreateUklibApiElements
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication.maybeCreateUklibRuntimeElements
import org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.uklibFragmentPlatformAttribute
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.wasmDecamelizedDefaultNameOrNull
import org.jetbrains.kotlin.gradle.utils.decamelize
import org.jetbrains.kotlin.gradle.utils.libsDirectory
import org.jetbrains.kotlin.gradle.utils.maybeCreateConsumable
import org.jetbrains.kotlin.gradle.utils.registerKlibArtifact

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

    target.createPublishArtifact(jsKlibTask, KLIB_TYPE, apiElements, runtimeElements)

    val klibProducingTask = mainCompilation.compileTaskProvider

    when (target.project.kotlinPropertiesProvider.kmpPublicationStrategy) {
        KmpPublicationStrategy.UklibPublicationInASingleComponentWithKMPPublication -> {
            val uklibAttribute = target.uklibFragmentPlatformAttribute.convertToStringForPublicationInUmanifest()
            listOf(
                mainCompilation.project.maybeCreateUklibApiElements(),
                mainCompilation.project.maybeCreateUklibRuntimeElements(),
            ).forEach {
                it.outgoing.variants {
                    val variant = it.maybeCreate(uklibAttribute)
                    variant.registerKlibArtifact(
                        klibProducingTask.map { it.klibOutput },
                        mainCompilation.compilationName,
                    )
                    variant.attributes {
                        it.attribute(uklibStateAttribute, uklibStateDecompressed)
                        it.attribute(uklibViewAttribute, uklibAttribute)
                    }
                }
            }
        }
        KmpPublicationStrategy.StandardKMPPublication -> {}
    }
}
