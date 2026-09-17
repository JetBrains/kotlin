@file:OptIn(DelicateTestFederationApi::class)

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.jetbrains.kotlin.testFederation.*

tasks.withType<Test>().configureEach {
    val testFederationExtension = testFederationExtension
    val currentDomain = testFederationDomains
    val areNightlyTestsEnabled = project.areNightlyTestsEnabled

    val testFederationClusters: Provider<Set<TestCluster>> = testFederationClusters
    val formattedClusters = testFederationClusters.map { clusters -> clusters.toArgumentString() }

    inputs.property(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled)
    inputs.property(TEST_FEDERATION_CLUSTERS_KEY, formattedClusters)

    val projectPath = project.buildTreePath
    val scan = project.extensions.getByType(DevelocityConfiguration::class).buildScan

    doFirst {
        this as Test

        scan.value("$projectPath:${this.name} domain", currentDomain.get().toString())
        scan.value("$projectPath:${this.name} test clusters", formattedClusters.get())

        val testFramework = testFramework
        val isJUnitPlatform = testFramework is JUnitPlatformTestFramework
        val testClusters = testFederationClusters.get()
        val smokeTests = testFederationExtension.smokeTests
        val contractTests = testFederationExtension.contractTests
        val runAllTestsAlways = testFederationExtension.runAllTestsAlways.get()
        val runAllTestsOrSkip = testFederationExtension.runAllTestsOrSkip.get()

        logger.quiet("Current Domain: '${currentDomain.get()}'")
        logger.quiet("Requested Test Clusters: '${formattedClusters.get()}'")

        if (!isJUnitPlatform && smokeTests.autoSamplePercentage.isPresent) {
            error("'includeAutoSamples' requires a JUnit 5 test task; task '$path' uses '${testFramework.javaClass.simpleName}'")
        }

        val declaresClusterFiltersInGradle = runAllTestsAlways ||
                smokeTests.testNamePatterns.get().isNotEmpty() ||
                contractTests.domains.isNotEmpty()

        if (!runAllTestsOrSkip && !isJUnitPlatform && !declaresClusterFiltersInGradle) {
            error(buildString {
                appendLine("Unsupported 'testFramework' found for task '$path'")
                appendLine("  testFramework: ${testFramework.javaClass.simpleName}; expected: '${JUnitPlatformTestFramework::class.simpleName}'")
                appendLine("  solutions:")
                appendLine("     - Use the 'project-tests-convention' testTask")
                appendLine("     - Use JUnit 5 by calling 'useJUnitPlatform()'")
                appendLine("     - Configure name-pattern selection: 'testFederation { smokeTests { includeTestsMatching(...) } }'")
                appendLine("     - Skip the task via: 'testFederation { runAllTestsOrSkip() }'")
            })
        }

        if (runAllTestsOrSkip && TestCluster.AllTests !in testClusters) {
            throw StopExecutionException(
                "The test task is disabled because it sets runAllTestsOrSkip() and the AllTests cluster was not requested." +
                        "Actually requested: $testClusters"
            )
        }

        if (TestCluster.AllTests !in testClusters) {
            if (TestCluster.SmokeTests in testClusters) {
                smokeTests.testNamePatterns.get().forEach { pattern -> filter.includeTestsMatching(pattern) }
            }
            contractTests.domains.forEach { domainSelection ->
                val testCluster = contractTestsClusterOf(Domain.valueOf(domainSelection.name))
                if (testCluster in testClusters) {
                    domainSelection.testNamePatterns.get().forEach { pattern -> filter.includeTestsMatching(pattern) }
                }
            }
        }

        if (!isJUnitPlatform) {
            return@doFirst
        }

        systemProperty(TEST_FEDERATION_CLUSTERS_KEY, formattedClusters.get())
        environment(TEST_FEDERATION_CLUSTERS_ENV_KEY, formattedClusters.get())

        systemProperty(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled.get())
        environment(TEST_FEDERATION_NIGHTLY_ENV_KEY, areNightlyTestsEnabled.get())

        smokeTests.autoSamplePercentage.orNull?.let { percentage ->
            systemProperty(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, percentage)
            environment(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY, percentage)
        }

        for (testCluster in testClusters) {
            println("##teamcity[addBuildTag '$testCluster']")
        }

        if (!areNightlyTestsEnabled.get()) {
            testFramework.options.excludeTags("nightly", "org.jetbrains.kotlin.testFederation.NightlyTest")
        }

        if (classpath.files.any { file -> file.name.contains("junit-vintage-engine") }) {
            error("Unsupported 'junit-vintage-engine' found in classpath. Please remove this dependency")
        }
    }
}

afterEvaluate {
    tasks.withType<Test>().configureEach {
        val defaultFailOnNoDiscoveredTests = failOnNoDiscoveredTests.get()
        failOnNoDiscoveredTests.value(testFederationClusters.map { clusters ->
            if (TestCluster.AllTests !in clusters) false
            else defaultFailOnNoDiscoveredTests
        }).disallowChanges()

        val clusters = testFederationClusters
        doFirst {
            if (TestCluster.AllTests !in clusters.get()) {
                filter.isFailOnNoMatchingTests = false
            }
        }
    }
}
