/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import java.util.concurrent.atomic.AtomicBoolean

internal fun <T> Project.whenEvaluated(fn: Project.() -> T) {
    if (state.executed) {
        fn()
        return
    }

    /** If there's already an Android plugin applied, just dispatch the action to `afterEvaluate`, it gets executed after AGP's actions */
    if (androidPluginIds.any { pluginManager.hasPlugin(it) }) {
        afterEvaluate { fn() }
        return
    }

    val isDispatchedAfterAndroid = AtomicBoolean(false)

    /**
     * This queue holds all actions submitted to `whenEvaluated` in this project, waiting for one of the Android plugins to be applied.
     * After (and if) an Android plugin gets applied, we dispatch all the actions in the queue to `afterEvaluate`, so that they are
     * executed after what AGP scheduled to `afterEvaluate`. There are different Android plugins, so actions in the queue also need to check
     * if it's the first Android plugin, using `isDispatched` (each has its own instance).
     */
    val afterAndroidDispatchQueue = project.extensions.extraProperties.getOrPut("org.jetbrains.kotlin.whenEvaluated") {
        val queue = mutableListOf<() -> Unit>()
        // Trigger the actions on any plugin applied; the actions themselves ensure that they only dispatch the fn once.
        androidPluginIds.forEach { id ->
            pluginManager.withPlugin(id) { queue.forEach { it() } }
        }
        queue
    }
    afterAndroidDispatchQueue.add {
        if (!isDispatchedAfterAndroid.getAndSet(true)) {
            afterEvaluate { fn() }
        }
    }

    afterEvaluate {
        /** If no Android plugin was loaded, then the action was not dispatched, and we can freely execute it now */
        if (!isDispatchedAfterAndroid.getAndSet(true)) {
            fn()
        }
    }
}