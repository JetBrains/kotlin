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

    /* Resolve the mode from the task configuration, overrides, and domain selection. */
    val testFederationMode: Provider<TestFederationMode> = testFederationMode

    inputs.property(TEST_FEDERATION_MODE_KEY, testFederationMode)
    inputs.property(SMOKE_TEST_CONFIG_KEY, smokeTestConfig)
    inputs.property(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled)

    /*
    Use changed domains as a task input only when they select individual tests.
    Full-mode runs do not use this selection, so their build cache entries can be reused across selections.
    */
    inputs.property(TEST_FEDERATION_CHANGED_DOMAINS_KEY, testFederationMode.zip(changedDomains) { mode, domains ->
        if (mode == TestFederationMode.Smoke) domains.toArgumentString() else "*"
    })

    val projectPath = project.buildTreePath
    val scan = project.extensions.getByType(DevelocityConfiguration::class).buildScan

    doFirst {
        this as Test

        scan.value("$projectPath:${this.name} domain", currentDomain.get().toString())
        scan.value("$projectPath:${this.name} changed domains", formattedChangedDomains.get())
        scan.value("$projectPath:${this.name} test mode", testFederationMode.get().toString())

        val testFramework = testFramework
        val smokeTestConfig = smokeTestConfig.get()

        logger.quiet("Current Domain: '${currentDomain.get()}'")
        logger.quiet("Changed Domains: '${formattedChangedDomains.get()}'")
        logger.quiet("Domain Test Mode: '${testFederationMode.get()}'")

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
        if (smokeTestConfig is SmokeTestConfig.Disabled && testFederationMode.get() == TestFederationMode.Smoke) {
            throw StopExecutionException("The test task is disabled in Smoke Test mode")
        }

        /*
        Run non-JUnit 5 tasks without further configuration in Full mode.
        These tasks must be configured as Disabled so they are skipped when no full run is selected.
        */
        if (testFramework !is JUnitPlatformTestFramework && testFederationMode.get() == TestFederationMode.Full) {
            return@doFirst
        }

        /* At this point we know that only JUnitPlatformTestFrameworks survive */
        testFramework as JUnitPlatformTestFramework

        /*
        Configure the test environment
         */
        systemProperty(TEST_FEDERATION_MODE_KEY, testFederationMode.get().name)
        environment(TEST_FEDERATION_MODE_ENV_KEY, testFederationMode.get().name)

        systemProperty(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled.get())
        environment(TEST_FEDERATION_NIGHTLY_ENV_KEY, areNightlyTestsEnabled.get())

        /*
        Provide changed domains only when the runtime uses them to select tests.
        Full-mode runs do not use this selection, so their build cache entries can be reused across selections.
        */
        if (testFederationMode.get() == TestFederationMode.Smoke) {
            systemProperty(TEST_FEDERATION_CHANGED_DOMAINS_KEY, formattedChangedDomains.get())
            environment(TEST_FEDERATION_CHANGED_DOMAINS_ENV_KEY, formattedChangedDomains.get())
        }

        if (smokeTestConfig is SmokeTestConfig.Enabled) {
            systemProperty(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, smokeTestConfig.autoSmokeTestPercentage)
            environment(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY, smokeTestConfig.autoSmokeTestPercentage)
        }

        /* Set TeamCity tags */
        if (testFederationMode.get() == TestFederationMode.Smoke) {
            println("##teamcity[addBuildTag 'Mode: Smoke']")
            changedDomains.get().forEach { domain ->
                println("##teamcity[addBuildTag 'Changed: $domain']")
            }
        } else {
            println("##teamcity[addBuildTag 'Mode: Full']")
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
        failOnNoDiscoveredTests.value(testFederationMode.map { mode ->
            if (mode == TestFederationMode.Smoke) false
            else defaultFailOnNoDiscoveredTests
        }).disallowChanges()

        val testFederationMode = testFederationMode
        doFirst {
            if (testFederationMode.get() == TestFederationMode.Smoke) {
                filter.isFailOnNoMatchingTests = false
            }
        }
    }
}
