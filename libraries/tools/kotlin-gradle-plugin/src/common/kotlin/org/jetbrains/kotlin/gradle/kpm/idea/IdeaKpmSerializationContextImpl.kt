/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaExtrasSerializer
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationLogger
import org.jetbrains.kotlin.tooling.core.Extras

internal fun IdeaSerializationContext(
    logger: Logger, extrasSerializationExtensions: List<IdeaExtrasSerializationExtension>
): IdeaSerializationContext {
    return IdeaSerializationContextImpl(
        extrasSerializationExtension = IdeaKpmCompositeExtrasSerializationExtension(logger, extrasSerializationExtensions),
        logger = IdeaKpmSerializationLoggerImpl(logger)
    )
}

private class IdeaSerializationContextImpl(
    override val extrasSerializationExtension: IdeaExtrasSerializationExtension,
    override val logger: IdeaSerializationLogger
) : IdeaSerializationContext

/* Simple composite implementation, reporting conflicting extensions */
private class IdeaKpmCompositeExtrasSerializationExtension(
    private val logger: Logger,
    private val extensions: List<IdeaExtrasSerializationExtension>
) : IdeaExtrasSerializationExtension {
    override fun <T : Any> serializer(key: Extras.Key<T>): IdeaExtrasSerializer<T>? {
        val serializers = extensions.mapNotNull { it.serializer(key) }

        if (serializers.size == 1) {
            return serializers.single()
        }

        if (serializers.size > 1) {
            logger.error("Conflicting serializers found for Extras.Key $key: $serializers")
            return null
        }

        return null
    }
}

/* Simple Gradle logger based implementation */
private class IdeaKpmSerializationLoggerImpl(
    private val logger: Logger,
) : IdeaSerializationLogger {
    override fun report(severity: IdeaSerializationLogger.Severity, message: String, cause: Throwable?) {
        val text = "[KPM] Serialization: $message"
        when (severity) {
            IdeaSerializationLogger.Severity.WARNING -> logger.warn(text, cause)
            IdeaSerializationLogger.Severity.ERROR -> logger.error(text, cause)
        }
    }
}
