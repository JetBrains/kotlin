@file:OptIn(DelicateTestFederationApi::class)

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.jetbrains.kotlin.testFederation.*

val extension = extensions.create<TestFederationExtension>("testFederation")

tasks.withType<Test>().configureEach {
    val currentDomain = testFederationDomains
    val changedDomains = project.testFederationChangedDomains
    val areNightlyTestsEnabled = project.areNightlyTestsEnabled

    val formattedChangedDomains = changedDomains.map { domains -> domains.toArgumentString() }
    val smokeTestConfig = smokeTestConfig

    /*
    'testFederationMode' is resolved only to forward 'test.federation.mode' to the test runtime for backward
    compatibility (some fixtures still read it directly). Every selection decision below uses subsets instead.
    */
    val testFederationMode: Provider<TestFederationMode> = testFederationMode

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
        scan.value("$projectPath:${this.name} changed domains", formattedChangedDomains.get())
        scan.value("$projectPath:${this.name} test clusters", formattedClusters.get())

        val testFramework = testFramework
        val smokeTestConfig = smokeTestConfig.get()

        logger.quiet("Current Domain: '${currentDomain.get()}'")
        logger.quiet("Changed Domains: '${formattedChangedDomains.get()}'")
        logger.quiet("Requested Clusters: '${formattedClusters.get()}'")

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
        Configure the test environment.
        'test.federation.mode' and 'test.federation.changed.domains' are forwarded only for backward
        compatibility with runtime consumers that still read them directly; selection itself is driven
        entirely by 'test.federation.subsets' below.
         */
        systemProperty(TEST_FEDERATION_MODE_KEY, testFederationMode.get().name)
        environment(TEST_FEDERATION_MODE_ENV_KEY, testFederationMode.get().name)

        systemProperty(TEST_FEDERATION_CLUSTERS_KEY, formattedClusters.get())
        environment(TEST_FEDERATION_CLUSTERS_ENV_KEY, formattedClusters.get())

        systemProperty(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled.get())
        environment(TEST_FEDERATION_NIGHTLY_ENV_KEY, areNightlyTestsEnabled.get())

        /*
        Provide changed domains only when a full test run is not selected.
        Full test runs do not use this selection, so their build cache entries can be reused across selections.
        */
        if (TestCluster.AllTests !in testFederationClusters.get()) {
            systemProperty(TEST_FEDERATION_CHANGED_DOMAINS_KEY, formattedChangedDomains.get())
            environment(TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY, formattedChangedDomains.get())
        }

        if (smokeTestConfig is SmokeTestConfig.Enabled) {
            systemProperty(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, smokeTestConfig.autoSmokeTestPercentage)
            environment(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY, smokeTestConfig.autoSmokeTestPercentage)
        }

        /* Set TeamCity tags */
        if (TestCluster.AllTests !in testFederationClusters.get()) {
            changedDomains.get().forEach { domain ->
                println("##teamcity[addBuildTag 'Changed: $domain']")
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
