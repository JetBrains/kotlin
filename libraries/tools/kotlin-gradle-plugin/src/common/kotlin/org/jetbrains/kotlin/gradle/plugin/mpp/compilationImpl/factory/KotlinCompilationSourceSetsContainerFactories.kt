/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.factory

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSourceSetsContainer
import org.jetbrains.kotlin.gradle.plugin.sources.android.kotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.utils.*

internal class DefaultKotlinCompilationSourceSetsContainerFactory(
    private val defaultSourceSetNaming: DefaultSourceSetNaming = DefaultDefaultSourceSetNaming
) : KotlinCompilationImplFactory.KotlinCompilationSourceSetsContainerFactory {

    fun interface DefaultSourceSetNaming {
        fun defaultSourceSetName(target: KotlinTarget, compilationName: String): String
    }

    object DefaultDefaultSourceSetNaming : DefaultSourceSetNaming {
        override fun defaultSourceSetName(target: KotlinTarget, compilationName: String): String = lowerCamelCaseName(
            target.disambiguationClassifier.takeIf { target !is KotlinMetadataTarget },
            if (compilationName == KotlinCompilation.MAIN_COMPILATION_NAME && target is KotlinMetadataTarget)
                KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME
            else compilationName
        )
    }

    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationSourceSetsContainer {
        val defaultSourceSetName = defaultSourceSetNaming.defaultSourceSetName(target, compilationName)
        val defaultSourceSet = target.project.kotlinExtension.sourceSets.maybeCreate(defaultSourceSetName)
        return KotlinCompilationSourceSetsContainer(defaultSourceSet)
    }
}

internal class AndroidCompilationSourceSetsContainerFactory(
    private val target: KotlinAndroidTarget,
    @Suppress("TYPEALIAS_EXPANSION_DEPRECATION") private val variant: DeprecatedAndroidBaseVariant
) : KotlinCompilationImplFactory.KotlinCompilationSourceSetsContainerFactory {
    override fun create(target: KotlinTarget, compilationName: String): KotlinCompilationSourceSetsContainer {
        val sourceSetName = target.project.kotlinAndroidSourceSetLayout.naming.defaultKotlinSourceSetName(this.target, variant)
            ?: lowerCamelCaseName(target.disambiguationClassifier, compilationName)
        return KotlinCompilationSourceSetsContainer(target.project.kotlinExtension.sourceSets.maybeCreate(sourceSetName))
    }
}