/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.utils.whenEvaluated

/**
 * This class encapsulated logic which should be invoked during not before the script evaluation is ready and
 * not earlier than the task is configured
 */
internal class RunOnceAfterEvaluated(private val name: String, private val action: () -> (Unit)) {
    private val logger = Logging.getLogger(this.javaClass)!!
    private var executed = false
    private var configured = false
    private var evaluated = false

    private fun execute() {
        logger.debug("[$name] RunOnceAfterEvaluated - execute executed=$executed evaluated=$evaluated configured=$configured")
        if (!executed) {
            logger.debug("[$name] RunOnceAfterEvaluated - EXECUTING executed=$executed evaluated=$evaluated configured=$configured")
            action()
        }
        executed = true
    }

    fun onEvaluated() {
        logger.debug("[$name] RunOnceAfterEvaluated - onEvaluated executed=$executed evaluated=$evaluated configured=$configured")
        evaluated = true
        if (configured) {
            execute()
        }
    }

    fun onConfigure() {
        logger.debug("[$name] RunOnceAfterEvaluated - onConfigure executed=$executed evaluated=$evaluated configured=$configured")
        configured = true
        if (evaluated) {
            execute()
        }
    }
}

internal fun Project.runOnceAfterEvaluated(name: String, action: () -> (Unit)) {
    val runOnce = RunOnceAfterEvaluated(name, action)
    whenEvaluated { runOnce.onEvaluated() }
    runOnce.onConfigure()
}
