/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.js

import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.toTargetPlatform

abstract class JsPlatform : SimplePlatform("JS") {
    override val oldFashionedDescription: String
        get() = "JavaScript "
}

@Suppress("DEPRECATION_ERROR")
object JsPlatforms {
    private object DefaultSimpleJsPlatform : JsPlatform()

    @Deprecated(
        message = "Should be accessed only by compatibility layer, other clients should use 'defaultJsPlatform'",
        level = DeprecationLevel.ERROR
    )
    object CompatJsPlatform : TargetPlatform(setOf(DefaultSimpleJsPlatform)),
        // Needed for backward compatibility, because old code uses INSTANCEOF checks instead of calling extensions
        org.jetbrains.kotlin.js.resolve.JsPlatform {}

    val defaultJsPlatform: TargetPlatform
        get() = CompatJsPlatform

    val allJsPlatforms: List<TargetPlatform> = listOf(defaultJsPlatform)
}

fun TargetPlatform?.isJs(): Boolean = this?.singleOrNull() is JsPlatform