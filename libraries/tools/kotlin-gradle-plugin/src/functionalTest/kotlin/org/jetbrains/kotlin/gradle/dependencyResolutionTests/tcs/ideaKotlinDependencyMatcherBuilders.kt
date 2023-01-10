/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.internal

fun dependsOnDependency(sourceSet: KotlinSourceSet) =
    org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.dependsOnDependency(sourceSet.internal.project, sourceSet.name)