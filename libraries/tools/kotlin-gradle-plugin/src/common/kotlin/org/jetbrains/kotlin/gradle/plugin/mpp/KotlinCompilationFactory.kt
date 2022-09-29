/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

interface KotlinCompilationFactory<T : KotlinCompilation<*>> : NamedDomainObjectFactory<T> {
    val project: Project get() = target.project
    val target: KotlinTarget
    val itemClass: Class<T>

    fun defaultSourceSetName(compilationName: String): String = lowerCamelCaseName(
        target.disambiguationClassifier.takeIf { target !is KotlinMetadataTarget },
        when {
            // corner case: main compilation of the metadata target compiles commonMain
            compilationName == KotlinCompilation.MAIN_COMPILATION_NAME &&
                    target is KotlinMetadataTarget -> KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME

            else -> compilationName
        }
    )
}

internal fun KotlinCompilationFactory<*>.getOrCreateDefaultSourceSet(compilationName: String): KotlinSourceSet {
    return project.kotlinExtension.sourceSets.maybeCreate(defaultSourceSetName(compilationName))
}
