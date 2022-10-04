/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSourceSetsContainer
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal object JsIrCompilationSourceSetsContainerFactory : KotlinCompilationImplFactory.KotlinCompilationSourceSetsContainerFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationSourceSetsContainer {
        return KotlinCompilationSourceSetsContainer(
            target.project.kotlinExtension.sourceSets.maybeCreate(defaultSourceSetName(target, compilationName))
        )
    }

    private fun defaultSourceSetName(target: KotlinTarget, compilationName: String): String {
        return lowerCamelCaseName(
            if (target is KotlinJsIrTarget && target.mixedMode)
                target.disambiguationClassifierInPlatform
            else target.disambiguationClassifier,
            compilationName
        )
    }
}
