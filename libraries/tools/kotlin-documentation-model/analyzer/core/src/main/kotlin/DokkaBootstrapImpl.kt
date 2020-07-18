package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.PackageOptions
import org.jetbrains.dokka.utilities.DokkaLogger

import java.util.function.BiConsumer


fun parsePerPackageOptions(args: List<String>): List<PackageOptions> = args.map { it.split(",") }.map {
    val prefix = it.first()
    if (prefix == "")
        throw IllegalArgumentException(
            "Please do not register packageOptions with all match pattern, use global settings instead"
        )

    val args = it.subList(1, it.size)

    val deprecated = args.find { it.endsWith("skipDeprecated") }?.startsWith("+")
        ?: DokkaDefaults.skipDeprecated

    val reportUndocumented = args.find { it.endsWith("reportUndocumented") }?.startsWith("+")
        ?: DokkaDefaults.reportUndocumented

    val privateApi = args.find { it.endsWith("includeNonPublic") }?.startsWith("+")
        ?: DokkaDefaults.includeNonPublic

    val suppress = args.find { it.endsWith("suppress") }?.startsWith("+")
        ?: DokkaDefaults.suppress

    PackageOptionsImpl(
        prefix,
        includeNonPublic = privateApi,
        reportUndocumented = reportUndocumented,
        skipDeprecated = !deprecated,
        suppress = suppress
    )
}


/**
 * Accessed with reflection
 */
@Suppress("unused")
class DokkaBootstrapImpl : DokkaBootstrap {

    class DokkaProxyLogger(val consumer: BiConsumer<String, String>) : DokkaLogger {
        override var warningsCount: Int = 0
        override var errorsCount: Int = 0

        override fun debug(message: String) {
            consumer.accept("debug", message)
        }

        override fun info(message: String) {
            consumer.accept("info", message)
        }

        override fun progress(message: String) {
            consumer.accept("progress", message)
        }

        override fun warn(message: String) {
            consumer.accept("warn", message).also { warningsCount++ }
        }

        override fun error(message: String) {
            consumer.accept("error", message).also { errorsCount++ }
        }
    }

    private lateinit var generator: DokkaGenerator

    fun configure(logger: DokkaLogger, configuration: DokkaConfigurationImpl) {
        generator = DokkaGenerator(configuration, logger)
    }

    override fun configure(serializedConfigurationJSON: String, logger: BiConsumer<String, String>) = configure(
        DokkaProxyLogger(logger),
        DokkaConfigurationImpl(serializedConfigurationJSON)
    )

    override fun generate() = generator.generate()
}
