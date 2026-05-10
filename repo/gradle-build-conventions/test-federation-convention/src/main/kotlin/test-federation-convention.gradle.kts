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

if (project.testFederationEnabled.orNull == true) {
    tasks.withType<Test>().configureEach {
        val currentDomain = project.testFederationDomain
        val affectedDomains = project.testFederationAffectedDomains
        val formattedAffectedDomains = affectedDomains.map { domains -> domains.toArgumentString() }
        val isSmokeTest = project.provider { isSmokeTest }

        /* If the task itself is marked as 'isSmokeTest', then it always has to be fully executed */
        val testFederationMode = project.testFederationMode.zip(isSmokeTest.orElse(false)) { mode, isSmokeTest ->
            if (isSmokeTest) TestFederationMode.Full
            else mode
        }

        inputs.property(TEST_FEDERATION_MODE_KEY, testFederationMode)
        inputs.property(TEST_FEDERATION_AFFECTED_DOMAINS_KEY, formattedAffectedDomains)
        inputs.property("test.federation.isSmokeTest", isSmokeTest).optional(true)

        val testFederationRuntime = testFederationRuntime

        doFirst {
            this as Test

            val isSmokeTest = isSmokeTest.orNull
            if (isSmokeTest == true) {
                logger.quiet("$path is marked as 'isSmokeTest'")
            }


            logger.quiet("Current Domain: '${currentDomain.get()}'")
            logger.quiet("Affected Domains: '${formattedAffectedDomains.get()}'")
            logger.quiet("Domain Test Mode: '${testFederationMode.get()}'")

            systemProperty(TEST_FEDERATION_MODE_KEY, testFederationMode.get().name)
            environment(TEST_FEDERATION_MODE_ENV_KEY, testFederationMode.get().name)

            systemProperty(TEST_FEDERATION_AFFECTED_DOMAINS_KEY, formattedAffectedDomains.get())
            environment(TEST_FEDERATION_AFFECTED_DOMAINS_ENV_KEY, formattedAffectedDomains.get())

            /* The test task was explicitly marked as 'isSmokeTest=false', therefore, won't further execute in smoke mode */
            if (isSmokeTest == false && testFederationMode.get() == TestFederationMode.Smoke) {
                throw StopExecutionException("Test task is marked as 'isSmokeTest=false' and therefore won't run in SmokeTest mode")
            }

            /* If the task itself is marked as 'isSmokeTest', then all tests within are considered a smoke test, no filters apply */
            if (testFederationMode.get() == TestFederationMode.Smoke) {
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
