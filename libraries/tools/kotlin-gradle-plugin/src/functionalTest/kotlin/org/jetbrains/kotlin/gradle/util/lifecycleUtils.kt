/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginLifecycle
import org.jetbrains.kotlin.tooling.core.withLinearClosure
import java.lang.AssertionError

fun Project.runLifecycleAwareTest(block: suspend Project.() -> Unit) {
    kotlinPluginLifecycle.launch { block() }
    try {
        (this as ProjectInternal).evaluate()
    } catch (t: Throwable) {
        /* Prefer throwing AssertionError directly, if possible */
        val allCauses = t.withLinearClosure { it.cause }
        allCauses.find { it is AssertionError }?.let { throw it }
        throw t
    }
}