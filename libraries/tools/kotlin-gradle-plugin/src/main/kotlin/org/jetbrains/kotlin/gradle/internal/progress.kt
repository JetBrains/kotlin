/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.logging.events.ProgressStartEvent
import org.gradle.internal.logging.progress.ProgressLogger

fun Project.operation(
    description: String,
    initialStatus: String? = null,
    body: ProgressLogger.() -> Unit
) {
    val services = (project as ProjectInternal).services
    val progressFactory = services.get(org.gradle.internal.logging.progress.ProgressLoggerFactory::class.java)
    val operation = progressFactory.newOperation(ProgressStartEvent.BUILD_OP_CATEGORY)
    operation.start(description, initialStatus)
    try {
        operation.body()
    } finally {
        operation.completed()
    }
}