/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmExtrasSerializer
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationLogger
import org.jetbrains.kotlin.tooling.core.Extras

internal fun IdeaKpmSerializationContext(
    logger: Logger, extrasSerializationExtensions: List<IdeaKpmExtrasSerializationExtension>
): IdeaKpmSerializationContext {
    return IdeaKpmSerializationContextImpl(
        extrasSerializationExtension = IdeaKpmCompositeExtrasSerializationExtension(logger, extrasSerializationExtensions),
        logger = IdeaKpmSerializationLoggerImpl(logger)
    )
}

private class IdeaKpmSerializationContextImpl(
    override val extrasSerializationExtension: IdeaKpmExtrasSerializationExtension,
    override val logger: IdeaKpmSerializationLogger
) : IdeaKpmSerializationContext

/* Simple composite implementation, reporting conflicting extensions */
private class IdeaKpmCompositeExtrasSerializationExtension(
    private val logger: Logger,
    private val extensions: List<IdeaKpmExtrasSerializationExtension>
) : IdeaKpmExtrasSerializationExtension {
    override fun <T : Any> serializer(key: Extras.Key<T>): IdeaKpmExtrasSerializer<T>? {
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
) : IdeaKpmSerializationLogger {
    override fun report(severity: IdeaKpmSerializationLogger.Severity, message: String, cause: Throwable?) {
        val text = "[KPM] Serialization: $message"
        when (severity) {
            IdeaKpmSerializationLogger.Severity.WARNING -> logger.warn(text, cause)
            IdeaKpmSerializationLogger.Severity.ERROR -> logger.error(text, cause)
        }
    }
}
