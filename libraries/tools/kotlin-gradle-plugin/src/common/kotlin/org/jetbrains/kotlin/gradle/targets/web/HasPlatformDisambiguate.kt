/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web

import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal interface HasPlatformDisambiguate {
    val platformDisambiguate: String?

    fun extensionName(baseName: String): String =
        lowerCamelCaseName(platformDisambiguate.orEmpty(), baseName)
}