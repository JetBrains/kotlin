/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.application.ApplicationManager

enum class ActionRunningMode {
    RUN_IN_CURRENT_THREAD,
    RUN_IN_EDT
}

inline fun <T> ActionRunningMode.runAction(crossinline action: () -> T): T = when (this) {
    ActionRunningMode.RUN_IN_CURRENT_THREAD -> {
        action()
    }
    ActionRunningMode.RUN_IN_EDT -> {
        var result: T? = null
        ApplicationManager.getApplication().invokeAndWait {
            result = ApplicationManager.getApplication().runWriteAction<T> { action() }
        }
        result!!
    }
}