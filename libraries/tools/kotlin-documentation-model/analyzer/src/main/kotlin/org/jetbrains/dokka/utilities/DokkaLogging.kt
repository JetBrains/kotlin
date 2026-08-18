/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.utilities

import java.util.concurrent.atomic.AtomicInteger

public interface DokkaLogger {
    public var warningsCount: Int
    public var errorsCount: Int

    /**
     * This level is for showing significant execution steps.
     *
     * What could be considered `progress`:
     * - Documentation generation started
     * - Documentation generation has successfully finished
     *
     * What could not be considered `progress`:
     * - Processing submodules
     * - Transforming pages
     *
     * These can be shown by default if there is no other way to track progress (like Gradle's progress bar),
     * and should be at the same level or one of the debug levels otherwise.
     *
     * Dokka's `progress` maps to:
     *
     * * CLI - shown by default
     * * Gradle - `info`
     * * Maven - `debug`
     */
    public fun progress(message: String)

    /**
     * This level is for logging non-user actionable messages,
     * like internal errors that cannot be fixed/worked around by users themselves or atomic generation steps.
     * These outputs could be attached to particular Dokka issues, so that the Dokka team could analyze them.
     *
     * What could be considered `debug`:
     * * Processing submodules
     * * Transforming
     *
     * What could not be considered `debug`:
     * * Cannot resolve a sample for $functionName: $fqLink
     *
     * Dokka's `debug` maps to:
     * * CLI - `debug`
     * * Gradle - `debug`
     * * Maven - `debug`
     */
    public fun debug(message: String)

    /**
     * This level is for logging useful messages about Dokka usage.
     *
     * What could be considered `info`:
     * * The HTML output is generated here: <PATH>
     *
     * What could not be considered `info`:
     * * Cannot resolve a sample for $functionName: $fqLink
     *
     * Dokka's `info` maps to:
     * * CLI - shown by default
     * * Gradle - `info`
     * * Maven - `info`
     */
    public fun info(message: String)

    /**
     * This level is for logging messages about issues during the documentation generation,
     * which do not stop the generation with an error but somehow affect the final result.
     * For example, if a particular source link could not be rendered.
     * It is mandatory for the messages of this level to be understandable to the user.
     *
     * What could be considered a `warn`:
     * * Cannot resolve a sample for $functionName: $fqLink. Please check the comments for the function.
     * The link should be formed according to the following rules:...
     *
     * What could not be considered a `warn`:
     * * Dokka is performing: $generationName
     *
     * Dokka's `warn` maps to:
     * * CLI - `warn`
     * * Gradle - `warn`
     * * Maven - `warn`
     */
    public fun warn(message: String)

    /**
     * This level is for logging error messages that describe what prevented Dokka from proceeding
     * with successful documentation generation. Likely, users will submit these error messages to Dokka's issues.
     *
     * What could be considered an `error`:
     * * Something went wrong, and the generation could not be performed. Please report it to the Dokka team.
     *
     * What could not be considered an `error`:
     * * Cannot resolve a sample for $functionName: $fqLink. Please check the comments for the function.
     * The link should be formed according to the following rules:...
     *
     * Dokka's `error` maps to:
     * * CLI - `error`
     * * Gradle - `error`
     * * Maven - `error`
     */
    public fun error(message: String)
}

public fun DokkaLogger.report() {
    if (warningsCount > 0 || errorsCount > 0) {
        info(
            "Generation completed with $warningsCount warning" +
                    (if (warningsCount == 1) "" else "s") +
                    " and $errorsCount error" +
                    if (errorsCount == 1) "" else "s"
        )
    } else {
        info("Generation completed successfully")
    }
}

public enum class LoggingLevel(
    public val index: Int
) {
    DEBUG(0), PROGRESS(1), INFO(2), WARN(3), ERROR(4);
}

/**
 * Used to decouple the transport layer from logger and make it convenient for testing
 */
public fun interface MessageEmitter : (String) -> Unit {
    public companion object {
        public val consoleEmitter: MessageEmitter = MessageEmitter { message -> println(message) }
    }
}

public class DokkaConsoleLogger(
    private val minLevel: LoggingLevel = LoggingLevel.PROGRESS,
    private val emitter: MessageEmitter = MessageEmitter.consoleEmitter
) : DokkaLogger {
    private val warningsCounter = AtomicInteger()
    private val errorsCounter = AtomicInteger()

    override var warningsCount: Int
        get() = warningsCounter.get()
        set(value) = warningsCounter.set(value)

    override var errorsCount: Int
        get() = errorsCounter.get()
        set(value) = errorsCounter.set(value)

    override fun debug(message: String) {
        if (shouldBeDisplayed(LoggingLevel.DEBUG)) emitter(message)
    }

    override fun progress(message: String) {
        if (shouldBeDisplayed(LoggingLevel.PROGRESS)) emitter("PROGRESS: $message")
    }

    override fun info(message: String) {
        if (shouldBeDisplayed(LoggingLevel.INFO)) emitter(message)
    }

    override fun warn(message: String) {
        if (shouldBeDisplayed(LoggingLevel.WARN)) {
            emitter("WARN: $message")
        }
        warningsCounter.incrementAndGet()
    }

    override fun error(message: String) {
        if (shouldBeDisplayed(LoggingLevel.ERROR)) {
            emitter("ERROR: $message")
        }
        errorsCounter.incrementAndGet()
    }

    private fun shouldBeDisplayed(messageLevel: LoggingLevel): Boolean = messageLevel.index >= minLevel.index
}
