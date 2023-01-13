/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.util.internal.VersionNumber
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.condition.OS

fun disabledOnWindowsWhenAgpVersionIsLowerThan(currentAgpVersion: String, minimalAgpVersion: String, reason: String? = null) {
    // isn't implemented via JUnit extension API because it doesn't allow accessing test parameters
    // see https://github.com/junit-team/junit5/issues/1139
    assumeFalse(
        OS.WINDOWS.isCurrentOs && VersionNumber.parse(currentAgpVersion) < VersionNumber.parse(minimalAgpVersion),
        reason ?: "Disabled on Windows because $currentAgpVersion is lower than $minimalAgpVersion"
    )
}