/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.process.ExecOperations
import javax.inject.Inject


@Deprecated("Temporary util to support changing the visibility of constructors of Gradle managed types. Instead, inject ExecOperations into the Gradle managed type. Scheduled for removal in Kotlin 2.4.")
internal abstract class ExecOpsHolder
@Inject
constructor(
    val execOps: ExecOperations,
)

@Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")
@Deprecated("Temporary util to support changing the visibility of constructors of Gradle managed types. Instead, inject ExecOperations into the Gradle managed type. Scheduled for removal in Kotlin 2.4.")
internal fun Project.getExecOperations(): ExecOperations =
    objects.newInstance(ExecOpsHolder::class.java).execOps
