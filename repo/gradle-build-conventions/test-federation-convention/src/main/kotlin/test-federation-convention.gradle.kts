import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.testFederation.*

val extension = extensions.create<TestFederationExtension>("systemTests")

if (project.testFederationEnabled.orNull == true) {
    tasks.withType<Test>().configureEach {
        val currentSubsystem = project.testFederationSubsystem
        val subsystemMode = project.testFederationMode
        val affectedSubsystems = project.testFederationAffectedSubsystems
        val isSmokeTest = project.provider { isSmokeTest }

        doFirst {
            if (isSmokeTest.get()) {
                logger.quiet("$path is marked as 'isSmokeTest'")
            }

            logger.quiet("Current Subsystem: '$currentSubsystem'")
            logger.quiet("Subsystem Test Mode: '${subsystemMode.get()}'")
            systemProperty(TEST_FEDERATION_MODE_KEY, subsystemMode.get().name)
            environment(TEST_FEDERATION_MODE_ENV, subsystemMode.get().name)

            val formattedAffectedSubsystems = affectedSubsystems.get().asArgumentString()
            logger.quiet("Affected Subsystems: '$formattedAffectedSubsystems'")
            systemProperty(TEST_FEDERATION_AFFECTED_SUBSYSTEMS_KEY, formattedAffectedSubsystems)
            environment(TEST_FEDERATION_AFFECTED_SUBSYSTEMS_ENV_KEY, formattedAffectedSubsystems)

            /* If the task itself is marked as 'isSmokeTest', then all tests within are considered a smoke test, no filters apply */
            if (!isSmokeTest.get() && subsystemMode.get() == TestFederationMode.Smoke) {
                val testFramework = testFramework
                if (testFramework is JUnitPlatformTestFramework) {
                    testFramework.options.includeTags("smoke")
                    affectedSubsystems.get().forEach { subsystem ->
                        testFramework.options.includeTags("contract:${subsystem.name}")
                    }
                }

                if (testFramework is JUnitTestFramework) {
                    testFramework.options.includeCategories("org.jetbrains.kotlin.testFederation.SmokeTest")
                }

                println("##teamcity[addBuildTag 'Subsystem Test Mode: Smoke']")
                affectedSubsystems.get().forEach { testSystem ->
                    println("##teamcity[addBuildTag 'Affected: $testSystem']")
                }
            }
        }
    }

    afterEvaluate {
        tasks.withType<Test>().configureEach {
            /*
            When running in smoke test mode, a given test task might actually not provide any smoke test
            */
            failOnNoDiscoveredTests.value(testFederationMode.map { it != TestFederationMode.Smoke })
        }
    }
}

afterEvaluate {
    if (extension.defaultDependencyEnabled.get()) {
        dependencies {
            configurations.findByName("testImplementation")?.name(project(":repo:test-federation-runtime"))
            configurations.findByName("jvmTestImplementation")?.name(project(":repo:test-federation-runtime"))
        }
    }
}

tasks.register<TestFederationContractDumpTask>("contractsDump") {
    classesDirs.from(tasks.withType<KotlinCompile>().map { it.destinationDirectory })
    group = "verification"
    description = "Dumps all 'Contract Tests' declared in the current project"
}
