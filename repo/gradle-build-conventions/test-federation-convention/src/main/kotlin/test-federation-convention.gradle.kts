@file:OptIn(DelicateTestFederationApi::class)

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.jetbrains.kotlin.testFederation.*

val extension = extensions.create<TestFederationExtension>("testFederation")

project.dependencies.extensions.add(
    ProjectDependency::class.java, "testFederationRuntime", dependencies.project(":repo:test-federation-runtime")
)

val testFederationRuntime = configurations.detachedConfiguration(dependencies.project(":repo:test-federation-runtime")).apply {
    isTransitive = false
}.incoming.files

tasks.withType<Test>().configureEach {
    val currentDomain = testFederationDomains
    val changedDomains = project.testFederationChangedDomains
    val areNightlyTestsEnabled = project.areNightlyTestsEnabled

    val formattedChangedDomains = changedDomains.map { domains -> domains.toArgumentString() }
    val smokeTestConfig = smokeTestConfig

    /* If the task itself is marked as 'isSmokeTest', then it always has to be fully executed */
    val testFederationMode: Provider<TestFederationMode> = testFederationMode

    inputs.property(TEST_FEDERATION_MODE_KEY, testFederationMode)
    inputs.property(SMOKE_TEST_CONFIG_KEY, smokeTestConfig)
    inputs.property(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled)

    /*
    We only use the exact set of domains as input to the test task if we're actually running in smoke test mode.
    This will allow for safely re-using build caches of any 'full mode' run.
    */
    inputs.property(TEST_FEDERATION_CHANGED_DOMAINS_KEY, testFederationMode.zip(changedDomains) { mode, domains ->
        if (mode == TestFederationMode.Smoke) domains.toArgumentString() else "*"
    })

    val testFederationRuntime = testFederationRuntime
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
        At this point: Assert that JUnit 5 is used, as 'Smoke Test' configurations use JUnit 5 features.
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

        /* The test task was explicitly marked as 'isSmokeTest=false', therefore, won't further execute in smoke mode */
        if (smokeTestConfig is SmokeTestConfig.Disabled && testFederationMode.get() == TestFederationMode.Smoke) {
            throw StopExecutionException("The test task is disabled in Smoke Test mode")
        }

        /*
        The test task is not using JUnit 5 and is scheduled for 'full mode' -> No further configuration required. Just run the vanilla task
        (we allow non-JUnit 5 tests for 'full' test mode, but not for Smoke Test mode)
        This effectively only allows non-JUnit 5 tests with SmokeTestConfig.Disabled
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
        We will only provide the 'affected domains' to the test task if we're actually running in smoke test mode.
        This will allow for safely re-using build caches of any 'full mode' run.
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

        /* Ensure that the test federation runtime is always available on the classpath (and the extension is enabled) */
        systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

        /* Check if classpath contains test federation runtime */
        if (!classpath.files.containsAll(testFederationRuntime.files)) {
            error("Test Federation Runtime is not available on the classpath")
        }

        /* Check if classpath contains vintage engine and report it as unsupported */
        if (classpath.files.any { file -> file.name.contains("junit-vintage-engine") }) {
            error("Unsupported 'junit-vintage-engine' found in classpath. Please remove this dependency")
        }
    }
}

afterEvaluate {
    tasks.withType<Test>().configureEach {
        classpath += testFederationRuntime
        /*
        When running in smoke test mode, a given test task might actually not provide any smoke test
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

afterEvaluate {
    if (extension.defaultDependencyEnabled.get()) {
        dependencies {
            configurations.findByName("testImplementation")?.name(project(":repo:test-federation-runtime"))
            configurations.findByName("jvmTestImplementation")?.name(project(":repo:test-federation-runtime"))
            configurations.findByName("testFixturesCompileOnly")?.name(project(":repo:test-federation-runtime"))
        }
    }
}
