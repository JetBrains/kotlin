@file:OptIn(DelicateTestFederationApi::class)

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.jetbrains.kotlin.testFederation.*
import org.jetbrains.kotlin.testFederation.TestSubset.*

tasks.withType<Test>().configureEach {
    val extension = testFederationExtension
    val domains = testFederationDomains
    val formattedDomains = domains.map { it.toArgumentString() }
    val subsets = testFederationSubsets
    val formattedSubsets = subsets.map { it.toArgumentString() }
    val areNightlyTestsEnabled = project.areNightlyTestsEnabled

    inputs.property(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled)
    inputs.property(TEST_FEDERATION_SUBSETS_KEY, formattedSubsets)

    val projectPath = project.buildTreePath
    val scan = project.extensions.getByType(DevelocityConfiguration::class).buildScan

    doFirst {
        val testFramework = testFramework

        logger.quiet(buildFrame(
            "Domain: ${domains.get()}",
            "Test subsets: [${formattedSubsets.get().replace(",", ", ")}]",
        ))
        scan.value("$projectPath:${this.name} domain", domains.get().toString())
        scan.value("$projectPath:${this.name} test subsets", formattedSubsets.get())

        if (testFramework !is JUnitPlatformTestFramework) {
            // Non-JUnit 5 tasks can't be split into subsets, so they must opt out of every partial subset
            check(extension.smokeTests.skip.get() && extension.contractTests.skip.get()) {
                buildString {
                    appendLine("Unsupported 'testFramework' found for task '$path'")
                    appendLine("  testFramework: ${testFramework.javaClass.simpleName}; expected: '${JUnitPlatformTestFramework::class.simpleName}'")
                    appendLine("  solutions:")
                    appendLine("     - Use the 'project-tests-convention' testTask")
                    appendLine("     - Use JUnit 5 by calling 'useJUnitPlatform()'")
                    appendLine("     - Opt out from every partial subset: 'testFederation { smokeTests { skip() }; contractTests { skip() } }'")
                }
            }
        }

        val shouldSkipTask = when {
            setOf(SmokeTests).containsAll(subsets.get()) -> extension.smokeTests.skip.get()
            contractSubsets.containsAll(subsets.get()) -> extension.contractTests.skip.get()
            (contractSubsets + SmokeTests).containsAll(subsets.get()) -> {
                extension.smokeTests.skip.get() && extension.contractTests.skip.get()
            }
            else -> false
        }
        if (shouldSkipTask) {
            val message = "The test task is disabled because all requested subsets are configured to skip()"
            logger.quiet(message)
            throw StopExecutionException(message)
        }

        if (testFramework !is JUnitPlatformTestFramework) {
            // Every partial subset is skipped above, so only AllTests can reach this point
            check(AllTests in subsets.get()) {
                "Unexpected test subsets for non-JUnit 5 task '$path': ${subsets.get()}"
            }
            // Run non-JUnit 5 tasks without further configuration
            return@doFirst
        }

        systemProperty(TEST_FEDERATION_DOMAINS_KEY, formattedDomains.get())
        environment(TEST_FEDERATION_DOMAINS_ENV_KEY, formattedDomains.get())

        systemProperty(TEST_FEDERATION_SUBSETS_KEY, formattedSubsets.get())
        environment(TEST_FEDERATION_SUBSETS_ENV_KEY, formattedSubsets.get())

        systemProperty(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled.get())
        environment(TEST_FEDERATION_NIGHTLY_ENV_KEY, areNightlyTestsEnabled.get())

        extension.smokeTests.autoSamplePercentage.orNull?.let { percentage ->
            systemProperty(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, percentage)
            environment(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY, percentage)
        }

        for (testSubset in subsets.get()) {
            println("##teamcity[addBuildTag '$testSubset']")
        }

        // Exclude nightly tests if not specifically running in 'nightly' mode
        if (!areNightlyTestsEnabled.get()) {
            testFramework.options.excludeTags("nightly", "org.jetbrains.kotlin.testFederation.NightlyTest")
        }

        // Check if classpath contains vintage engine and report it as unsupported
        if (classpath.files.any { file -> file.name.contains("junit-vintage-engine") }) {
            error("Unsupported 'junit-vintage-engine' found in classpath. Please remove this dependency")
        }
    }
}

afterEvaluate {
    tasks.withType<Test>().configureEach {
        // A task that selects only a subset of tests may have no tests to run.
        val defaultFailOnNoDiscoveredTests = failOnNoDiscoveredTests.get()
        failOnNoDiscoveredTests.value(testFederationSubsets.map { subsets ->
            if (!subsets.contains(AllTests)) false
            else defaultFailOnNoDiscoveredTests
        }).disallowChanges()

        val subsets = testFederationSubsets
        doFirst {
            if (!subsets.get().contains(AllTests)) {
                filter.isFailOnNoMatchingTests = false
            }
        }
    }
}
