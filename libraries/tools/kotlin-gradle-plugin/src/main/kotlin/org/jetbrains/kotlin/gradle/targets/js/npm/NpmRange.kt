/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import com.github.gundy.semver4j.model.Version

data class NpmRange(
    val startVersion: Version? = null,
    val startInclusive: Boolean = false,
    val endVersion: Version? = null,
    val endInclusive: Boolean = false
)

val NONE_RANGE = NpmRange(
    startVersion = Version.fromString(NONE_VERSION)
)

infix operator fun NpmRange.plus(other: NpmRange) {

}