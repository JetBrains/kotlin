/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.internal.logging.events.ProgressStartEvent
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.service.ServiceRegistry

fun <T> ServiceRegistry.operation(
    description: String,
    initialStatus: String? = null,
    body: ProgressLogger.() -> T
): T {
    val progressFactory = get(org.gradle.internal.logging.progress.ProgressLoggerFactory::class.java)
    val operation = progressFactory.newOperation(ProgressStartEvent.BUILD_OP_CATEGORY)
    operation.start(description, initialStatus)
    try {
        return operation.body()
    } finally {
        operation.completed()
    }
}