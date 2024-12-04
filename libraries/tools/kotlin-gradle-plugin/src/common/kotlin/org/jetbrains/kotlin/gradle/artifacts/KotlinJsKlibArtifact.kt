/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.artifacts

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.wasmDecamelizedDefaultNameOrNull
import org.jetbrains.kotlin.gradle.utils.decamelize
import org.jetbrains.kotlin.gradle.utils.libsDirectory

internal val KotlinJsKlibArtifact = KotlinTargetArtifact { target, apiElements, runtimeElements ->
    if (target !is KotlinJsIrTarget) return@KotlinTargetArtifact

    val jsKlibTask = target.createArtifactsTask {
        it.from(target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).output.allOutputs)
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
}
