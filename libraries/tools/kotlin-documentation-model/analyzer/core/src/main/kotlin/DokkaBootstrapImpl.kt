package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.PackageOptions
import ru.yole.jkid.deserialization.deserialize
import java.io.File
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

    override fun configure(logger: BiConsumer<String, String>, serializedConfigurationJSON: String)
            = configure(DokkaProxyLogger(logger), deserialize<DokkaConfigurationImpl>(serializedConfigurationJSON))

    fun configure(logger: DokkaLogger, configuration: DokkaConfiguration) = with(configuration) {
        generator = DokkaGenerator(
                logger,
                classpath,
                sourceRoots,
                samples,
                includes,
                moduleName,
                DocumentationOptions(
                    outputDir = outputDir,
                    outputFormat = format,
                    includeNonPublic = includeNonPublic,
                    includeRootPackage = includeRootPackage,
                    reportUndocumented = reportUndocumented,
                    skipEmptyPackages = skipEmptyPackages,
                    skipDeprecated = skipDeprecated,
                    jdkVersion = jdkVersion,
                    generateIndexPages = generateIndexPages,
                    sourceLinks = sourceLinks,
                    impliedPlatforms = impliedPlatforms,
                    perPackageOptions = perPackageOptions,
                    externalDocumentationLinks = externalDocumentationLinks,
                    noStdlibLink = noStdlibLink,
                    noJdkLink = noJdkLink,
                    languageVersion = languageVersion,
                    apiVersion = apiVersion,
                    cacheRoot = cacheRoot,
                    suppressedFiles = suppressedFiles.map { File(it) }.toSet(),
                    collectInheritedExtensionsFromLibraries = collectInheritedExtensionsFromLibraries
                )
        )
    }

    override fun generate() = generator.generate()
}