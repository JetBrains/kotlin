/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.HasPlatformDisambiguate

internal object JsPlatformDisambiguate : HasPlatformDisambiguate {
    override val platformDisambiguate: String?
        get() = null

    internal val jsPlatform: String
        get() = KotlinPlatformType.js.name
}