@file:OptIn(DelicateTestFederationApi::class)

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.jetbrains.kotlin.testFederation.*

val extension = extensions.create<TestFederationExtension>("testFederation")

tasks.withType<Test>().configureEach {
    val currentDomain = testFederationDomains
    val areNightlyTestsEnabled = project.areNightlyTestsEnabled

    val smokeTestConfig = smokeTestConfig

    val testFederationClusters: Provider<Set<TestCluster>> = testFederationClusters
    val formattedClusters = testFederationClusters.map { subsets -> subsets.toArgumentString() }

    inputs.property(SMOKE_TEST_CONFIG_KEY, smokeTestConfig)
    inputs.property(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled)
    inputs.property(TEST_FEDERATION_CLUSTERS_KEY, formattedClusters)

    val projectPath = project.buildTreePath
    val scan = project.extensions.getByType(DevelocityConfiguration::class).buildScan

    doFirst {
        this as Test

        scan.value("$projectPath:${this.name} domain", currentDomain.get().toString())
        scan.value("$projectPath:${this.name} test clusters", formattedClusters.get())

        val testFramework = testFramework
        val smokeTestConfig = smokeTestConfig.get()

        logger.quiet("Current Domain: '${currentDomain.get()}'")
        logger.quiet("Requested Test Subsets: '${formattedClusters.get()}'")

        /*
        Require JUnit 5 unless the task is configured to skip runs that select only a subset of tests.
        */
        if (testFramework !is JUnitPlatformTestFramework && smokeTestConfig !is SmokeTestConfig.Disabled) {
            error(buildString {
                appendLine("Unsupported 'testFramework' found for task '$path'")
                appendLine("  testFramework: ${testFramework.javaClass.simpleName}; expected: '${JUnitPlatformTestFramework::class.simpleName}'")
                appendLine("  solutions:")
                appendLine("     - Use the 'project-tests-convention' testTask")
                appendLine("     - Use JUnit 5 by calling 'useJUnitPlatform()'")
                appendLine("     - Disable the task for smoke tests: 'smokeTestConfig = SmokeTestConfig.Disabled'")
            })
        }

        /* Skip a task configured as Disabled when it is not selected for a full test run. */
        if (smokeTestConfig is SmokeTestConfig.Disabled && TestCluster.AllTests !in testFederationClusters.get()) {
            throw StopExecutionException("The test task is disabled because a full test run was not selected")
        }

        /*
        Run non-JUnit 5 tasks without further configuration when a full test run is selected.
        These tasks must be configured as Disabled so they are skipped otherwise.
        */
        if (testFramework !is JUnitPlatformTestFramework && TestCluster.AllTests in testFederationClusters.get()) {
            return@doFirst
        }

        /* At this point we know that only JUnitPlatformTestFrameworks survive */
        testFramework as JUnitPlatformTestFramework

        /*
        Configure the test environment
         */
        systemProperty(TEST_FEDERATION_CLUSTERS_KEY, formattedClusters.get())
        environment(TEST_FEDERATION_CLUSTERS_ENV_KEY, formattedClusters.get())

        systemProperty(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled.get())
        environment(TEST_FEDERATION_NIGHTLY_ENV_KEY, areNightlyTestsEnabled.get())

        if (smokeTestConfig is SmokeTestConfig.Enabled) {
            systemProperty(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, smokeTestConfig.autoSmokeTestPercentage)
            environment(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY, smokeTestConfig.autoSmokeTestPercentage)
        }

        for (testSubset in testFederationClusters.get()) {
            println("##teamcity[addBuildTag '$testSubset']")
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
        val defaultFailOnNoDiscoveredTests = failOnNoDiscoveredTests.get()
        failOnNoDiscoveredTests.value(testFederationClusters.map { subsets ->
            if (TestCluster.AllTests !in subsets) false
            else defaultFailOnNoDiscoveredTests
        }).disallowChanges()

        val testFederationClusters = testFederationClusters
        doFirst {
            if (TestCluster.AllTests !in testFederationClusters.get()) {
                filter.isFailOnNoMatchingTests = false
            }
        }
    }
}
