/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Internal

/**
 * Task is using Kotlin daemon to run compilation.
 */
interface CompileUsingKotlinDaemon : Task {
    /**
     * Provides JVM arguments to Kotlin daemon, default is `null` if "kotlin.daemon.jvmargs" property is not set.
     */
    @get:Internal
    val kotlinDaemonJvmArguments: ListProperty<String>
}
