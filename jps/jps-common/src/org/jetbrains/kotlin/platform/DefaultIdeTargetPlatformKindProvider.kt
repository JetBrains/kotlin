/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.config.isJps
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms

interface DefaultIdeTargetPlatformKindProvider {
    val defaultPlatform: TargetPlatform

    companion object {
        val defaultPlatform: TargetPlatform
            get() {
                if (isJps) {
                    // TODO support passing custom platforms in JPS
                    return JvmPlatforms.defaultJvmPlatform
                }

                return ApplicationManager.getApplication().getService(DefaultIdeTargetPlatformKindProvider::class.java).defaultPlatform
            }
    }
}

fun TargetPlatform?.orDefault(): TargetPlatform {
    return this ?: DefaultIdeTargetPlatformKindProvider.defaultPlatform
}

fun IdePlatformKind?.orDefault(): IdePlatformKind {
    return this ?: DefaultIdeTargetPlatformKindProvider.defaultPlatform.idePlatformKind
}