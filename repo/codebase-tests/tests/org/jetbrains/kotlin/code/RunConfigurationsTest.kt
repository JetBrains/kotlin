/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.kotlin.repoTestFixtures.isGitIgnored
import org.jetbrains.kotlin.testFederation.NightlyTest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.w3c.dom.Element
import java.io.File
import java.nio.file.Path
import java.util.stream.Stream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.streams.asStream

@NightlyTest
class RunConfigurationsTest {
    @TestFactory
    fun `execute Gradle --dry-run`(): Stream<DynamicTest> {
        return Path(".idea/runConfigurations").listDirectoryEntries("*.xml")
            .asSequence()
            .filter { configFile -> !configFile.isGitIgnored() }
            .mapNotNull { configFile -> parseGradleRunConfiguration(configFile) }
            .map { config ->
                DynamicTest.dynamicTest(config.name) {
                    testDryRunConfiguration(config)
                }
            }.asStream()
    }

    private fun testDryRunConfiguration(config: GradleRunConfiguration) {
        val projectDir = config.projectPath.replace("\$PROJECT_DIR\$", ".")

        val arguments = buildList {
            addAll(config.taskNames)
            addAll(config.scriptParameters.split("\\s+".toRegex()))
            add("--no-configuration-cache")
            add("--dry-run")
        }.filter { it.isNotBlank() }

        GradleRunner.create()
            .withProjectDir(File(projectDir).absoluteFile)
            .withEnvironment(System.getenv().plus(config.environmentVariables))
            .withArguments(arguments)
            .forwardOutput()
            .withTestKitDir(File(System.getProperty("gradle.user.home") ?: error("Missing 'gradle.user.home'")))
            .build()
    }
}

private data class GradleRunConfiguration(
    val configurationFile: Path,
    val name: String,
    val projectPath: String,
    val taskNames: List<String>,
    val scriptParameters: String,
    val environmentVariables: Map<String, String>,
)

private fun parseGradleRunConfiguration(configFile: Path): GradleRunConfiguration? {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(configFile.inputStream())

    val configuration = document.documentElement.getElementsByTagName("configuration")
        .let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }
        .firstOrNull() ?: return null

    if (configuration.getAttribute("type") != "GradleRunConfiguration") return null

    val name = configuration.getAttribute("name")

    val options = configuration.getElementsByTagName("option")
        .let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }

    fun findOptionValue(optionName: String): String =
        options.firstOrNull { it.getAttribute("name") == optionName }
            ?.getAttribute("value").orEmpty()

    val projectPath = findOptionValue("externalProjectPath")

    val taskNames = options.firstOrNull { it.getAttribute("name") == "taskNames" }
        ?.getElementsByTagName("option")
        ?.let { nodes -> (0 until nodes.length).map { i -> (nodes.item(i) as Element).getAttribute("value") } }
        .orEmpty()

    val scriptParameters = findOptionValue("scriptParameters")

    val environmentVariables = options.firstOrNull { it.getAttribute("name") == "env" }
        ?.getElementsByTagName("entry")
        ?.let { nodes ->
            (0 until nodes.length).associate { i ->
                val entry = nodes.item(i) as Element
                entry.getAttribute("key") to entry.getAttribute("value")
            }
        }.orEmpty()

    return GradleRunConfiguration(
        configurationFile = configFile,
        name = name,
        projectPath = projectPath,
        taskNames = taskNames,
        scriptParameters = scriptParameters,
        environmentVariables = environmentVariables,
    )
}
