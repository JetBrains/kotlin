/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

/** Beta versions of Gradle and AGP are checking in multiplatform related tests.
 *  Bumped manually.
 */
const val BETA_GRADLE = TestVersions.Gradle.G_8_0
const val BETA_AGP = TestVersions.AGP.AGP_80

/**
 * Base class for all Multiplatform Gradle plugin Kotlin integration tests.
 */
@AndroidTestVersions(additionalVersions = [BETA_AGP])
@GradleTestVersions(maxVersion = BETA_GRADLE, additionalVersions = [TestVersions.Gradle.MAX_SUPPORTED])
abstract class MPPBaseTest : KGPBaseTest() {
}