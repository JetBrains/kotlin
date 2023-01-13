/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializer
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationLogger
import org.jetbrains.kotlin.tooling.core.Extras

internal fun IdeaSerializationContext(
    logger: Logger, extrasSerializationExtensions: List<IdeaKotlinExtrasSerializationExtension>
): IdeaKotlinSerializationContext {
    return IdeaKotlinSerializationContextImpl(
        extrasSerializationExtension = IdeaKpmCompositeExtrasSerializationExtension(logger, extrasSerializationExtensions),
        logger = IdeaKpmSerializationLoggerImpl(logger)
    )
}

private class IdeaKotlinSerializationContextImpl(
    override val extrasSerializationExtension: IdeaKotlinExtrasSerializationExtension,
    override val logger: IdeaKotlinSerializationLogger
) : IdeaKotlinSerializationContext

/* Simple composite implementation, reporting conflicting extensions */
private class IdeaKpmCompositeExtrasSerializationExtension(
    private val logger: Logger,
    private val extensions: List<IdeaKotlinExtrasSerializationExtension>
) : IdeaKotlinExtrasSerializationExtension {
    override fun <T : Any> serializer(key: Extras.Key<T>): IdeaKotlinExtrasSerializer<T>? {
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
) : IdeaKotlinSerializationLogger {
    override fun report(severity: IdeaKotlinSerializationLogger.Severity, message: String, cause: Throwable?) {
        val text = "[KPM] Serialization: $message"
        when (severity) {
            IdeaKotlinSerializationLogger.Severity.WARNING -> logger.warn(text, cause)
            IdeaKotlinSerializationLogger.Severity.ERROR -> logger.error(text, cause)
        }
    }
}
