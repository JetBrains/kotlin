/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.targets.js.npm.FILE_VERSION_PREFIX
import org.jetbrains.kotlin.gradle.targets.js.npm.includedRange
import org.jetbrains.kotlin.gradle.targets.js.npm.intersect
import java.io.File

private const val SEPARATOR = "@"

private const val FILE_MARKER = "${SEPARATOR}$FILE_VERSION_PREFIX"
private const val GITHUB_MARKER = "${SEPARATOR}github:"