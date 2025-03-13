/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.targets.js

import kotlinx.coroutines.runBlocking
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import org.apache.velocity.runtime.RuntimeConstants.RESOURCE_LOADER
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

fun main() {
    val outputSourceRoot = System.getProperties()["org.jetbrains.kotlin.generators.gradle.targets.js.outputSourceRoot"]
    val npmPackageRoot = System.getProperties()["org.jetbrains.kotlin.generators.gradle.targets.js.npmPackageRoot"]
    val packageName = "org.jetbrains.kotlin.gradle.targets.js"
    val className = "NpmVersions"
    val fileName = "$className.kt"
    val targetFile = File("$outputSourceRoot")
        .resolve(packageName.replace(".", "/"))
        .resolve(fileName)

    val context = VelocityContext()
        .apply {
            put("package", packageName)
            put("class", className)
            put("license", File("license/COPYRIGHT_HEADER.txt").takeIf { it.exists() }?.readText() ?: "")
        }

    val velocityEngine = VelocityEngine().apply {
        setProperty(RESOURCE_LOADER, "class")
        setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
        init()
    }

    val template = velocityEngine.getTemplate("$fileName.vm")

    val packages = VersionFetcher().use {
        runBlocking {
            it.fetch()
        }
    }

    val dependencies = findLastVersions(packages)
        .also {
            context.put("dependencies", it)
        }

    targetFile.writer().use {
        template.merge(context, it)
    }

    val packageJson = File("$npmPackageRoot")
        .also { it.mkdirs() }
        .resolve("package.json")

    val packageJsonTemplate = velocityEngine.getTemplate("package.json.vm")

    val md = MessageDigest.getInstance("MD5")
    dependencies.forEach { (name, version) ->
        md.update(name.toByteArray(StandardCharsets.UTF_8))
        md.update(version.toByteArray(StandardCharsets.UTF_8))
    }

    val packageJsonContext = VelocityContext()
        .apply {
            @OptIn(ExperimentalStdlibApi::class)
            put("version", md.digest().toHexString())
            put("dependencies", dependencies.map { "    \"${it.name}\": \"${it.version}\"" })
            put("newline", "\n");
        }

    packageJson.writer().use {
        packageJsonTemplate.merge(packageJsonContext, it)
    }
}

fun findLastVersions(packages: List<PackageInformation>): List<Package> {
    return packages
        .map { packageInformation ->
            val maximumVersion = when (packageInformation) {
                is RealPackageInformation -> packageInformation.versions
                    .map { SemVer.from(it) }
                    .filter { it.preRelease == null && it.build == null }
                    .maxOrNull()
                    ?.toString()
                    ?: error("There is no applicable version for ${packageInformation.name}")
                is HardcodedPackageInformation -> packageInformation.version
            }

            Package(
                packageInformation.name,
                maximumVersion,
                packageInformation.displayName
            )
        }
}
