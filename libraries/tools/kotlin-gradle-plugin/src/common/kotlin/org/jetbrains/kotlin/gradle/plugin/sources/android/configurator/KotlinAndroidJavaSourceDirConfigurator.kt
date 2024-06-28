/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin.sources.android.configurator

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.utils.DeprecatedAndroidSourceSet
import org.jetbrains.kotlin.gradle.utils.fileCollectionFromConfigurableFileTree
import org.jetbrains.kotlin.gradle.utils.filesProvider

internal object KotlinAndroidJavaSourceDirConfigurator : KotlinAndroidSourceSetConfigurator {
    override fun configure(target: KotlinAndroidTarget, kotlinSourceSet: KotlinSourceSet, androidSourceSet: DeprecatedAndroidSourceSet) {
        val project = target.project

        kotlinSourceSet.kotlin.srcDir(project.provider {
            // getSourceDirectoryTrees() is not lazy, thus it should be wrapped with provider
            androidSourceSet.java.getSourceDirectoryTrees().map { sources ->
                project.fileCollectionFromConfigurableFileTree(sources)
            }
        })
    }
}