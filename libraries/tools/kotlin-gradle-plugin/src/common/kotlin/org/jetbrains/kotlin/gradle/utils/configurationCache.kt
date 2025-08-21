/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Task

@Deprecated(
    "Internal utility which is no longer required. The minimum Gradle version required by KGP is 7.6, which means `Task#notCompatibleWithConfigurationCache` can be used directly." +
            "Scheduled for removal in Kotlin 2.4.",
    ReplaceWith("notCompatibleWithConfigurationCache(reason)"),
)
fun Task.notCompatibleWithConfigurationCacheCompat(reason: String) {
    notCompatibleWithConfigurationCache(reason)
}
