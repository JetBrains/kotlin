/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.util.Path

@Deprecated(
    "This function is an internal Kotlin Gradle Plugin utility that is no longer used. Scheduled for removal in Kotlin 2.4.",
)
@Suppress("DEPRECATION")
tailrec fun Path.topRealPath(): Path {
    val parent = parent
    parent?.parent ?: return this

    return parent.topRealPath()
}
