/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal.state

import org.gradle.api.logging.Logger
import java.lang.ref.WeakReference
import java.util.HashMap

// todo: remove when https://github.com/gradle/gradle/issues/2678 is resolved
internal object TaskLoggers {
    private val taskLoggers = HashMap<String, WeakReference<Logger>>()

    @Synchronized
    fun put(path: String, logger: Logger) {
        taskLoggers[path] = WeakReference(logger)
    }

    @Synchronized
    fun get(path: String): Logger? =
        taskLoggers[path]?.get()

    @Synchronized
    fun clear() {
        taskLoggers.clear()
    }
}