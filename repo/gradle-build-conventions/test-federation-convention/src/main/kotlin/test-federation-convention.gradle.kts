@file:OptIn(DelicateTestFederationApi::class)

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.jetbrains.kotlin.testFederation.*
import org.jetbrains.kotlin.testFederation.TestSubset.SmokeTests

tasks.withType<Test>().configureEach {
    val testFederationExtension = testFederationExtension
    val currentDomain = testFederationDomains
    val areNightlyTestsEnabled = project.areNightlyTestsEnabled

    val testSubsets = testFederationSubsets
    val formattedSubsets = testSubsets.map { it.toArgumentString() }

    inputs.property(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled)
    inputs.property(TEST_FEDERATION_SUBSETS_KEY, formattedSubsets)

    val projectPath = project.buildTreePath
    val scan = project.extensions.getByType(DevelocityConfiguration::class).buildScan

    fun buildFrame(vararg lines: String): String {
        val title = "TEST FEDERATION"
        val length = lines.maxOf { it.length } + 2
        val borderLength = if ((length - title.length) % 2 == 0) length else length + 1
        val topBorderLength = (borderLength - title.length) / 2
        val topBorder = "┌" + "─".repeat(topBorderLength) + title + "─".repeat(topBorderLength) + "┐"
        val bottomBorder = "└" + "─".repeat(borderLength) + "┘"
        val formattedLines = lines.joinToString("\n") { "│ ${it.padEnd(borderLength - 2)} │" }
        return topBorder + "\n" + formattedLines + "\n" + bottomBorder
    }

    doFirst {
        val testFramework = testFramework
        val isJUnitPlatform = testFramework is JUnitPlatformTestFramework
        val testSubsets = testSubsets.get()
        val smokeTests = testFederationExtension.smokeTests
        val contractTests = testFederationExtension.contractTests
        val notCompatibleWithTestFederation = smokeTests.skip.get() || contractTests.skip.get()

        logger.quiet(buildFrame(
            "Current domain: ${currentDomain.get()}",
            "Test subsets: [${formattedSubsets.get().replace(",", ", ")}]",
        ))

        scan.value("$projectPath:${this.name} domain", currentDomain.get().toString())
        scan.value("$projectPath:${this.name} test subsets", formattedSubsets.get())

        if (!isJUnitPlatform && smokeTests.autoSamplePercentage.isPresent) {
            error("'includeAutoSamples' requires a JUnit 5 test task; task '$path' uses '${testFramework.javaClass.simpleName}'")
        }

        if (!notCompatibleWithTestFederation && !isJUnitPlatform) {
            error(buildString {
                appendLine("Unsupported 'testFramework' found for task '$path'")
                appendLine("  testFramework: ${testFramework.javaClass.simpleName}; expected: '${JUnitPlatformTestFramework::class.simpleName}'")
                appendLine("  solutions:")
                appendLine("     - Use the 'project-tests-convention' testTask")
                appendLine("     - Use JUnit 5 by calling 'useJUnitPlatform()'")
                appendLine("     - Configure name-pattern selection: 'testFederation { smokeTests { includeAutoSamples(...) } }'")
                appendLine("     - Skip the task via: 'testFederation { smokeTests { skip() }; contractTests { skip() } }'")
            })
        }

        val shouldSkipTask = when {
            testSubsets == setOf(SmokeTests) -> smokeTests.skip.get()
            ALL_CONTRACTS.containsAll(testSubsets) -> contractTests.skip.get()
            (ALL_CONTRACTS + SmokeTests).containsAll(testSubsets) -> smokeTests.skip.get() && contractTests.skip.get()
            else -> false
        }
        if (shouldSkipTask) {
            throw StopExecutionException(
                "The test task is disabled because all requested subsets are configured with skip(): $testSubsets"
            )
        }

        if (!isJUnitPlatform) {
            return@doFirst
        }

        systemProperty(TEST_FEDERATION_SUBSETS_KEY, formattedSubsets.get())
        environment(TEST_FEDERATION_SUBSETS_ENV_KEY, formattedSubsets.get())

        systemProperty(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled.get())
        environment(TEST_FEDERATION_NIGHTLY_ENV_KEY, areNightlyTestsEnabled.get())

        smokeTests.autoSamplePercentage.orNull?.let { percentage ->
            systemProperty(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, percentage)
            environment(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY, percentage)
        }

        val formattedSubsetsStr = formattedSubsets.get()
        if (formattedSubsetsStr == "*") {
            println("##teamcity[addBuildTag '*']")
        } else {
            for (testSubset in testSubsets) {
                println("##teamcity[addBuildTag '$testSubset']")
            }
        }

        /* Exclude nightly tests if not specifically running in 'nightly' mode */
        if (!areNightlyTestsEnabled.get()) {
            testFramework.options.excludeTags("nightly", "org.jetbrains.kotlin.testFederation.NightlyTest")
        }

        /* Check if classpath contains vintage engine and report it as unsupported */
        if (classpath.files.any { file -> file.name.contains("junit-vintage-engine") }) {
            error("Unsupported 'junit-vintage-engine' found in classpath. Please remove this dependency")
        }
    }
}

afterEvaluate {
    tasks.withType<Test>().configureEach {
        /*
        A task that selects only a subset of tests may have no tests to run.
        */
        val defaultFailOnNoDiscoveredTests = failOnNoDiscoveredTests.get()
        failOnNoDiscoveredTests.value(testFederationSubsets.map { subsets ->
            if (!subsets.toSet().containsAll(TestSubset.entries)) false
            else defaultFailOnNoDiscoveredTests
        }).disallowChanges()

        val subsets = testFederationSubsets
        doFirst {
            if (!subsets.get().toSet().containsAll(TestSubset.entries)) {
                filter.isFailOnNoMatchingTests = false
            }
        }
    }
}
