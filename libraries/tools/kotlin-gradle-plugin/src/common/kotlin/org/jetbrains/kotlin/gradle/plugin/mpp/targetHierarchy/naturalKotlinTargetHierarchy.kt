/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyDescriptor
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest

internal val naturalKotlinTargetHierarchy = KotlinTargetHierarchyDescriptor {
    if (!compilation.isMain() && !compilation.isTest()) {
        /* This hierarchy is only defined for default 'main' and 'test' compilations */
        return@KotlinTargetHierarchyDescriptor
    }

    common {
        if (isNative) {
            group("native") {
                if (isApple) {
                    group("apple") {
                        if (isIos) group("ios")
                        if (isTvos) group("tvos")
                        if (isWatchos) group("watchos")
                        if (isMacos) group("macos")
                    }
                }

                if (isLinux) group("linux")
                if (isWindows) group("windows")
                if (isAndroidNative) group("androidNative")
            }
        }
    }
}
