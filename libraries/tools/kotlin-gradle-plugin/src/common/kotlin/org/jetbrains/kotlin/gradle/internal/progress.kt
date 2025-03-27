/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.model.ObjectFactory
import org.gradle.internal.logging.events.ProgressStartEvent
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import javax.inject.Inject


/**
 * Create a new instance of a [ProgressLogger] with a category of [ProgressStartEvent.BUILD_OP_CATEGORY].
 *
 * **NOTE**: [ProgressLogger] cannot be stored in the Configuration Cache.
 * A new instance should only be created and used during the execution phase.
 */
internal fun ObjectFactory.newBuildOpLogger(): ProgressLogger =
    progressLoggerFactory()
        // I don't know how important the category is.
        // The KGP/JS code used it - I just wanted to preserve the existing behaviour.
        .newOperation(ProgressStartEvent.BUILD_OP_CATEGORY)


/**
 * Executes [body] within the context of a [ProgressLogger],
 * calling [ProgressLogger.started] before and [ProgressLogger.completed] after.
 */
internal fun <T> ProgressLogger.operation(
    description: String,
    initialStatus: String? = null,
    body: ProgressLogger.() -> T,
): T {
    start(description, initialStatus)
    try {
        return body(this)
    } finally {
        completed()
    }
}


/**
 * Instantiate a new instance of [ProgressLoggerFactory].
 *
 * **NOTE**: [ProgressLoggerFactory] cannot be stored in the Configuration Cache.
 * A new instance should only be created and used during the execution phase.
 */
private fun ObjectFactory.progressLoggerFactory(): ProgressLoggerFactory {
    val accessor = newInstance(ProgressLoggerFactoryAccessor::class.java)
    return accessor.progressLoggerFactory
}


/**
 * Used by [ObjectFactory.progressLoggerFactory] to access [ProgressLoggerFactory]
 * during the execution phase.
 *
 * Is there a way to hide this?
 * I would make this `private` if I could, but then [ObjectFactory] can't create a new instance.
 */
internal interface ProgressLoggerFactoryAccessor {
    @get:Inject
    val progressLoggerFactory: ProgressLoggerFactory
}
