@file:Suppress("SameParameterValue")

package org.jetbrains.dokka

import org.jetbrains.dokka.generation.GracefulGenerationExit
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.utilities.DokkaLogger

/**
 * DokkaGenerator is the main entry point for generating documentation
 *
 * [generate] method has been split into submethods for test reasons
 */
class DokkaGenerator(
    private val configuration: DokkaConfiguration,
    private val logger: DokkaLogger
) {
    fun generate() = timed(logger) {
        printDokkaMaturityWarning(logger)

        report("Initializing plugins")
        val context = initializePlugins(configuration, logger)

        context.single(CoreExtensions.generation).run {
            logger.progress("Dokka is performing: $generationName")
            generate()
        }

    }.dump("\n\n === TIME MEASUREMENT ===\n")

    fun initializePlugins(
        configuration: DokkaConfiguration,
        logger: DokkaLogger,
        additionalPlugins: List<DokkaPlugin> = emptyList()
    ) = DokkaContext.create(configuration, logger, additionalPlugins)
}

class Timer internal constructor(startTime: Long, private val logger: DokkaLogger?) {
    private val steps = mutableListOf("" to startTime)

    fun report(name: String) {
        logger?.progress(name)
        steps += (name to System.currentTimeMillis())
    }

    fun dump(prefix: String = "") {
        logger?.info(prefix)
        val namePad = steps.map { it.first.length }.maxOrNull() ?: 0
        val timePad = steps.windowed(2).map { (p1, p2) -> p2.second - p1.second }.maxOrNull()?.toString()?.length ?: 0
        steps.windowed(2).forEach { (p1, p2) ->
            if (p1.first.isNotBlank()) {
                logger?.info("${p1.first.padStart(namePad)}: ${(p2.second - p1.second).toString().padStart(timePad)}")
            }
        }
    }
}

private fun timed(logger: DokkaLogger? = null, block: Timer.() -> Unit): Timer =
    Timer(System.currentTimeMillis(), logger).apply {
        try {
            block()
        } catch (exit: GracefulGenerationExit) {
            report("Exiting Generation: ${exit.reason}")
        } finally {
            report("")
        }
    }

