@file:OptIn(TemporaryTestFederationApi::class)

import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
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
    val currentDomain = project.testFederationDomain
    val affectedDomains = project.testFederationAffectedDomains
    val formattedAffectedDomains = affectedDomains.map { domains -> domains.toArgumentString() }
    val smokeTestConfig = project.provider { smokeTestConfig }.orElse(SmokeTestConfig.Default)

    /* If the task itself is marked as 'isSmokeTest', then it always has to be fully executed */
    val testFederationMode = project.testFederationMode

    inputs.property(TEST_FEDERATION_MODE_KEY, testFederationMode)
    inputs.property(SMOKE_TEST_CONFIG_KEY, smokeTestConfig)

    /*
    We only use the exact set of domains as input to the test task if we're actually running in smoke test mode.
    This will allow for safely re-using build caches of any 'full mode' run.
     */
    inputs.property(TEST_FEDERATION_AFFECTED_DOMAINS_KEY, testFederationMode.zip(affectedDomains) { mode, domains ->
        if (mode == TestFederationMode.Smoke) domains.toArgumentString() else "*"
    })

    val testFederationRuntime = testFederationRuntime

    doFirst {
        this as Test

        val smokeTestConfig = smokeTestConfig.get()

        logger.quiet("Current Domain: '${currentDomain.get()}'")
        logger.quiet("Affected Domains: '${formattedAffectedDomains.get()}'")
        logger.quiet("Domain Test Mode: '${testFederationMode.get()}'")

        systemProperty(TEST_FEDERATION_MODE_KEY, testFederationMode.get().name)
        environment(TEST_FEDERATION_MODE_ENV_KEY, testFederationMode.get().name)

        /*
        We will only provide the 'affected domains' to the test task if we're actually running in smoke test mode.
        This will allow for safely re-using build caches of any 'full mode' run.
        */
        if (testFederationMode.get() == TestFederationMode.Smoke) {
            systemProperty(TEST_FEDERATION_AFFECTED_DOMAINS_KEY, formattedAffectedDomains.get())
            environment(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY, formattedAffectedDomains.get())
        }

        if (smokeTestConfig is SmokeTestConfig.Enabled) {
            systemProperty(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, smokeTestConfig.autoSmokeTestPercentage)
            environment(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY, smokeTestConfig.autoSmokeTestPercentage)
        }

        /* The test task was explicitly marked as 'isSmokeTest=false', therefore, won't further execute in smoke mode */
        if (smokeTestConfig is SmokeTestConfig.Disabled && testFederationMode.get() == TestFederationMode.Smoke) {
            throw StopExecutionException("The test task is disabled in Smoke Test mode")
        }

        if (testFederationMode.get() == TestFederationMode.Smoke) {
            smokeTestConfig as SmokeTestConfig.Enabled

            /*
            If we only execute tagged smoke/contract tests, then we can already add those tags as includes.
            The 'SmokeTestExecutionCondition' would also filter relevant tests, however adding a filter here can lead to
            a better rendering of the executed tests.
            */
            if (smokeTestConfig.autoSmokeTestPercentage == 0) {
                val testFramework = testFramework
                if (testFramework is JUnitPlatformTestFramework) {
                    testFramework.options.includeTags("smoke")
                    affectedDomains.get().forEach { domain ->
                        testFramework.options.includeTags("affectedBy:${domain.name}")
                    }
                }

                if (testFramework is JUnitTestFramework) {
                    testFramework.options.includeCategories("org.jetbrains.kotlin.testFederation.SmokeTest")
                }
            }

            println("##teamcity[addBuildTag 'Test Federation Mode: Smoke']")
            affectedDomains.get().forEach { domain ->
                println("##teamcity[addBuildTag 'Affected: $domain']")
            }
        }

        /* Ensure that the test federation runtime is always available on the classpath (and the extension is enabled) */
        systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

        /* Check if classpath contains test federation runtime */
        if (!classpath.files.containsAll(testFederationRuntime.files)) {
            error("Test Federation Runtime is not available on the classpath")
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

        val testFederationMode = project.testFederationMode
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
