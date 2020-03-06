/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.internal

import javax.inject.Inject
import org.gradle.process.ExecOperations

/**
 * Available since Gradle 6.0
 */
open class GradleExecOperationsHolder @Inject constructor(val execOperation: ExecOperations) {
}