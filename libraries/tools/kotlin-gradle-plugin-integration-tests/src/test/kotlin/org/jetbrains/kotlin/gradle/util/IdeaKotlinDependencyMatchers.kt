/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.isNativeDistribution
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.IdeaKotlinDependencyMatcher
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates

val kotlinStdlibDependencies = binaryCoordinates(Regex(".*kotlin-stdlib.*"))

val jetbrainsAnnotationDependencies = binaryCoordinates(Regex("org\\.jetbrains:annotations:.*"))

val kotlinNativeDistributionDependencies = IdeaKotlinDependencyMatcher("native distribution") { dependency ->
    dependency is IdeaKotlinResolvedBinaryDependency && dependency.isNativeDistribution
}