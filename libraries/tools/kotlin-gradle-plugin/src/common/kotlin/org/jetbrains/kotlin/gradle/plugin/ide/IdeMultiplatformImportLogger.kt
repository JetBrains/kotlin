package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.logging.Logger
import java.io.Serializable

internal class IdeMultiplatformImportLogger(
    val logger: Logger,
) {
    class Warning(val message: String, val cause: Throwable? = null) : Serializable
    class Error(val message: String, val cause: Throwable? = null) : Serializable

    private val collectedErrors: MutableList<Error> = mutableListOf()
    private val collectedWarnings: MutableList<Warning> = mutableListOf()

    fun error(
        message: String,
        cause: Throwable? = null,
    ) {
        logger.error(message, cause)
        collectedErrors.add(Error(message, cause))
    }

    fun warn(
        message: String,
        cause: Throwable? = null,
    ) {
        logger.warn(message, cause)
        collectedWarnings.add(Warning(message, cause))
    }

    class Events(
        val errors: List<Error>,
        val warnings: List<Warning>,
    ) : Exception()

    fun throwIfErrorsOrWarningsAreNotEmpty() {
        if (collectedErrors.isNotEmpty() || collectedWarnings.isNotEmpty()) {
            throw Events(
                errors = collectedErrors,
                warnings = collectedWarnings
            )
        }
    }
}