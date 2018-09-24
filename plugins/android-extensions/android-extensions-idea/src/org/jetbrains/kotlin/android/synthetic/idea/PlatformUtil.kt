/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.synthetic.idea

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.PlatformModuleInfo
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

internal fun ModuleInfo.findAndroidModuleInfo(): ModuleSourceInfo? {
    return when (this) {
        is ModuleSourceInfo -> this.takeIf { it.platform is JvmPlatform }
        is PlatformModuleInfo -> this.platformModule.takeIf { it.platform is JvmPlatform }
        else -> null
    }
}