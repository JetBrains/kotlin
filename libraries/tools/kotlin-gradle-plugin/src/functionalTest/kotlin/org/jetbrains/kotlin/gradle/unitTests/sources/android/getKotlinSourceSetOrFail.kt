/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.unitTests.sources.android

import com.android.build.gradle.api.AndroidSourceSet
import com.android.builder.model.SourceProvider
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.utils.androidExtension
import kotlin.test.fail

fun Project.getKotlinSourceSetOrFail(androidSourceSet: AndroidSourceSet): KotlinSourceSet {
    val kotlinSourceSets = kotlinExtension.sourceSets
        .filter { kotlinSourceSet -> kotlinSourceSet.androidSourceSetInfoOrNull?.androidSourceSetName == androidSourceSet.name }

    if (kotlinSourceSets.isEmpty()) {
        fail("Missing KotlinSourceSet for AndroidSourceSet: ${androidSourceSet.name}")
    }

    if (kotlinSourceSets.size > 1) {
        fail(
            "More than one KotlinSourceSet associated with AndroidSourceSet: ${androidSourceSet.name}. " +
                    "KotlinSourceSets: ${kotlinSourceSets.map { it.name }}"
        )
    }

    return kotlinSourceSets.single()
}

fun Project.getKotlinSourceSetOrFail(androidSourceSet: SourceProvider): KotlinSourceSet {
    return getKotlinSourceSetOrFail(androidExtension.sourceSets.getByName(androidSourceSet.name))
}
