/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.platform.impl.JvmIdePlatformKind

interface DefaultIdeTargetPlatformKindProvider {
    val defaultPlatform: IdePlatform<*, *>

    companion object {
        val defaultPlatform: IdePlatform<*, *>
            get() {
                if (ApplicationManager.getApplication() == null) {
                    // TODO support passing custom platforms in JPS
                    return JvmIdePlatformKind.defaultPlatform
                }

                return ServiceManager.getService(DefaultIdeTargetPlatformKindProvider::class.java).defaultPlatform
            }
    }
}