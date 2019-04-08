package org.jetbrains.dokka

import com.google.gson.Gson
import org.jetbrains.dokka.DokkaConfiguration.PackageOptions

import java.util.function.BiConsumer


fun parsePerPackageOptions(arg: String): List<PackageOptions> {
    if (arg.isBlank()) return emptyList()

    return arg.split(";").map { it.split(",") }.map {
        val prefix = it.first()
        if (prefix == "")
            throw IllegalArgumentException("Please do not register packageOptions with all match pattern, use global settings instead")
        val args = it.subList(1, it.size)
        val deprecated = args.find { it.endsWith("deprecated") }?.startsWith("+") ?: true
        val reportUndocumented = args.find { it.endsWith("warnUndocumented") }?.startsWith("+") ?: true
        val privateApi = args.find { it.endsWith("privateApi") }?.startsWith("+") ?: false
        val suppress = args.find { it.endsWith("suppress") }?.startsWith("+") ?: false
        PackageOptionsImpl(prefix, includeNonPublic = privateApi, reportUndocumented = reportUndocumented, skipDeprecated = !deprecated, suppress = suppress)
    }
}

class DokkaBootstrapImpl : DokkaBootstrap {

    private class DokkaProxyLogger(val consumer: BiConsumer<String, String>) : DokkaLogger {
        override fun info(message: String) {
            consumer.accept("info", message)
        }

        override fun warn(message: String) {
            consumer.accept("warn", message)
        }

        override fun error(message: String) {
            consumer.accept("error", message)
        }
    }

    lateinit var generator: DokkaGenerator
    val gson = Gson()

    fun configure(logger: DokkaLogger, configuration: DokkaConfigurationImpl) = with(configuration) {

        fun defaultLinks(config: PassConfigurationImpl): List<ExternalDocumentationLinkImpl> {
            val links = mutableListOf<ExternalDocumentationLinkImpl>()
            if (!config.noJdkLink)
                links += DokkaConfiguration.ExternalDocumentationLink
                    .Builder("https://docs.oracle.com/javase/${config.jdkVersion}/docs/api/")
                    .build() as ExternalDocumentationLinkImpl

            if (!config.noStdlibLink)
                links += DokkaConfiguration.ExternalDocumentationLink
                    .Builder("https://kotlinlang.org/api/latest/jvm/stdlib/")
                    .build() as ExternalDocumentationLinkImpl
            return links
        }

        val configurationWithLinks =
            configuration.copy(passesConfigurations =
            passesConfigurations
                .map {
                    val links: List<ExternalDocumentationLinkImpl> = it.externalDocumentationLinks + defaultLinks(it)
                    it.copy(externalDocumentationLinks = links)
                }
            )

        generator = DokkaGenerator(configurationWithLinks, logger)
    }

    override fun configure(logger: BiConsumer<String, String>, serializedConfigurationJSON: String)
            = configure(DokkaProxyLogger(logger), gson.fromJson(serializedConfigurationJSON, DokkaConfigurationImpl::class.java))

    override fun generate() = generator.generate()
}
