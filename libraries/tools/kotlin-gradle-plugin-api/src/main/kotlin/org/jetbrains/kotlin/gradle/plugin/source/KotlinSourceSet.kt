/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.source

import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies

interface KotlinSourceSet : Named, HasKotlinDependencies {
    val kotlin: SourceDirectorySet

    fun kotlin(configureClosure: Closure<Any?>): SourceDirectorySet

    companion object {
        const val COMMON_MAIN_SOURCE_SET_NAME = "commonMain"
        const val COMMON_TEST_SOURCE_SET_NAME = "commonTest"
    }
}

interface KotlinSourceSetWithResources : KotlinSourceSet {
    val resources: SourceDirectorySet
}