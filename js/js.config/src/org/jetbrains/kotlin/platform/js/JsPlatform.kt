/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.js

import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isJs as _isJs

object JsPlatforms {
    object DefaultSimpleJsPlatform : JsPlatform()

    private object CompatJsPlatform : TargetPlatform(setOf(DefaultSimpleJsPlatform))

    val defaultJsPlatform: TargetPlatform
        get() = CompatJsPlatform

    val allJsPlatforms: List<TargetPlatform> = listOf(defaultJsPlatform)
}
