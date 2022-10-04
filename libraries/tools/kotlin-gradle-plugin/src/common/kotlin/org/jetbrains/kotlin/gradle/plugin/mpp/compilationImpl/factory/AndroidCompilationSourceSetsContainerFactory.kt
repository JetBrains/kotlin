/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import com.android.build.gradle.api.BaseVariant
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSourceSetsContainer
import org.jetbrains.kotlin.gradle.plugin.sources.android.kotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal class AndroidCompilationSourceSetsContainerFactory(
    private val target: KotlinAndroidTarget, private val variant: BaseVariant
) : KotlinCompilationImplFactory.KotlinCompilationSourceSetsContainerFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationSourceSetsContainer {
        val sourceSetName = target.project.kotlinAndroidSourceSetLayout.naming.defaultKotlinSourceSetName(this.target, variant)
            ?: lowerCamelCaseName(target.disambiguationClassifier, compilationName)
        return KotlinCompilationSourceSetsContainer(target.project.kotlinExtension.sourceSets.maybeCreate(sourceSetName))
    }
}