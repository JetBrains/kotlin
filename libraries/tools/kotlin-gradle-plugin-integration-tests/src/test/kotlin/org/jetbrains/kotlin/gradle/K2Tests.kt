/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import kotlin.test.Ignore

@Disabled("Used for local testing only")
@MppGradlePluginTests
@DisplayName("K2: Hierarchical multiplatform")
class K2HierarchicalMppIT : HierarchicalMppIT() {
    override val defaultBuildOptions: BuildOptions get() = super.defaultBuildOptions.copy(languageVersion = "2.0")
}

@Ignore
class K2KlibBasedMppIT : KlibBasedMppIT() {
    override fun defaultBuildOptions(): BuildOptions = super.defaultBuildOptions().copy(languageVersion = "2.0")
}

@Ignore
class K2NewMultiplatformIT : NewMultiplatformIT() {
    override fun defaultBuildOptions(): BuildOptions = super.defaultBuildOptions().copy(languageVersion = "2.0")
}

@Ignore
class K2CommonizerIT : CommonizerIT() {
    override fun defaultBuildOptions(): BuildOptions = super.defaultBuildOptions().copy(languageVersion = "2.0")
}

@Ignore
class K2CommonizerHierarchicalIT : CommonizerHierarchicalIT() {
    override fun defaultBuildOptions(): BuildOptions = super.defaultBuildOptions().copy(languageVersion = "2.0")
}
