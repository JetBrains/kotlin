package org.jetbrains.dokka

import java.io.File
import java.util.function.BiConsumer

fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinition {
    val (path, urlAndLine) = srcLink.split('=')
    return SourceLinkDefinition(File(path).absolutePath,
            urlAndLine.substringBefore("#"),
            urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#" + it })
}

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
        PackageOptions(prefix, includeNonPublic = privateApi, reportUndocumented = reportUndocumented, skipDeprecated = !deprecated)
    }
}

fun parseSourceRoot(sourceRoot: String): SourceRoot {
    val components = sourceRoot.split("::", limit = 2)
    return SourceRoot(components.last(), if (components.size == 1) listOf() else components[0].split(','))
}

class DokkaBootstrapImpl : DokkaBootstrap {

    class DokkaProxyLogger(val consumer: BiConsumer<String, String>) : DokkaLogger {
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

    override fun configure(logger: BiConsumer<String, String>,
                           moduleName: String,
                           classpath: List<String>,
                           sources: List<String>,
                           samples: List<String>,
                           includes: List<String>,
                           outputDir: String,
                           format: String,
                           includeNonPublic: Boolean,
                           includeRootPackage: Boolean,
                           reportUndocumented: Boolean,
                           skipEmptyPackages: Boolean,
                           skipDeprecated: Boolean,
                           jdkVersion: Int,
                           generateIndexPages: Boolean,
                           sourceLinks: List<String>) {
        generator = DokkaGenerator(
                DokkaProxyLogger(logger),
                classpath,
                sources.map(::parseSourceRoot),
                samples,
                includes,
                moduleName,
                DocumentationOptions(
                        outputDir,
                        format,
                        includeNonPublic,
                        includeRootPackage,
                        reportUndocumented,
                        skipEmptyPackages,
                        skipDeprecated,
                        jdkVersion,
                        generateIndexPages,
                        sourceLinks.map(::parseSourceLinkDefinition)
                )
        )

    }

    override fun generate() = generator.generate()
}