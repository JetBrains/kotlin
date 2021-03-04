@file:Suppress("SameParameterValue")

package org.jetbrains.dokka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.scheduling.ExperimentalCoroutineDispatcher
import org.jetbrains.dokka.generation.GracefulGenerationExit
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.utilities.DokkaLogger
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

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
        report("Initializing plugins")
        val context = initializePlugins(configuration, logger)

        runCatching {
            context.single(CoreExtensions.generation).run {
                logger.progress("Dokka is performing: $generationName")
                generate()
            }
        }.exceptionOrNull()?.let { e ->
            finalizeCoroutines()
            throw e
        }

        finalizeCoroutines()
    }.dump("\n\n === TIME MEASUREMENT ===\n")

    fun initializePlugins(
        configuration: DokkaConfiguration,
        logger: DokkaLogger,
        additionalPlugins: List<DokkaPlugin> = emptyList()
    ) = DokkaContext.create(configuration, logger, additionalPlugins)

    private fun finalizeCoroutines() {
        runCatching {
            Dispatchers.Default.closeExecutor()

            Dispatchers.IO.let { dispatcher ->
                dispatcher::class.memberProperties.find {
                    it.name == "dispatcher"
                }?.also {
                    it.isAccessible = true
                }?.call(dispatcher)
            }?.closeExecutor()
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    private fun Any.closeExecutor() = (this as ExperimentalCoroutineDispatcher).also {
        it.executor::class.members
            .find { it.name == "close" }
            ?.also {
                it.isAccessible = true
            }?.call(it.executor)
    }
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

