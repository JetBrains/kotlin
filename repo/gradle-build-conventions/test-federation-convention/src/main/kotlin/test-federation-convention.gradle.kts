@file:OptIn(DelicateTestFederationApi::class)
@file:Suppress("UnstableApiUsage")

import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.jetbrains.kotlin.testFederation.*
import org.jetbrains.kotlin.testFederation.TestSubset.*

tasks.withType<Test>().configureEach {
    val extension = testFederationExtension
    val domains = testFederationDomains
    val subsets = testFederationSubsets
    val areNightlyTestsEnabled = project.areNightlyTestsEnabled
    val projectPath = project.buildTreePath
    val scan = project.extensions.getByType(DevelocityConfiguration::class).buildScan

    inputs.property(TEST_FEDERATION_SUBSETS_KEY, subsets.formatted())
    inputs.property(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled)

    doFirst {
        printHeader(domains, subsets)
        setBuildScanValues(scan, projectPath, domains, subsets)
        checkSelfContracts(domains, extension)

        checkTestFramework(extension)
        skipPartialSubsetsIfNotCompatible(subsets, extension)
        if (!isAtLeastJUnit5()) {
            // Run non-JUnit 5 tasks without further configuration
            return@doFirst
        }

        // Only JUnit 5+ can reach this point
        val testFramework = testFramework as JUnitPlatformTestFramework

        setRuntimeProperties(domains, subsets, areNightlyTestsEnabled, extension)
        setTeamcityTags(domains, subsets)
        excludeNightlyTests(areNightlyTestsEnabled, testFramework)
        preventVintageEngineOnClasspath()
    }
}

afterEvaluate {
    tasks.withType<Test>().configureEach {
        configureFailingOnNonDiscoveredTests()
    }
}

fun Task.printHeader(domains: ListProperty<Domain>, subsets: Provider<Set<TestSubset>>) {
    logger.quiet(
        buildFrame(
            "Domain: ${domains.get()}",
            "Test subsets: [${subsets.formatted().get().replace(",", ", ")}]",
        )
    )
}

fun Task.setBuildScanValues(
    scan: BuildScanConfiguration,
    projectPath: String,
    domains: ListProperty<Domain>,
    subsets: Provider<Set<TestSubset>>,
) {
    scan.value("$projectPath:${this.name} domain", domains.formatted().get())
    scan.value("$projectPath:${this.name} test subsets", subsets.formatted().get())
}

fun Task.checkTestFramework(extension: TestFederationExtension) {
    if (!isAtLeastJUnit5()) {
        check(extension.notCompatibleReason.isPresent) {
            """
                Unsupported test framework for task '$path'
                
                HOW TO FIX:
                  - Migrate the task to JUnit 5+, or
                  - Mark the task as 'notCompatibleWithTestFederation()'
                """.trimIndent()
        }
    }
}

fun Test.isAtLeastJUnit5() = testFramework is JUnitPlatformTestFramework

fun Task.checkSelfContracts(domains: ListProperty<Domain>, extension: TestFederationExtension) {
    domains.get().map(::contractTestsSubsetOf).intersect(extension.declaredSubsets.get())
        .takeIf { it.isNotEmpty() }
        ?.let { selfContracts ->
            error(
                """
                    Task '$path' declares the contract test subsets of its own domains: ${selfContracts.toArgumentString()}.
                    
                    HOW TO FIX:
                      - Remove the respective susbet declarations from 'testFederation { }'
                    """.trimIndent()
            )
        }
}

fun Task.skipPartialSubsetsIfNotCompatible(subsets: Provider<Set<TestSubset>>, extension: TestFederationExtension) {
    if (AllTests !in subsets.get() && extension.notCompatibleReason.isPresent) {
        val message = buildString {
            append("Skipping tests because partial subset was requested and this task is not compatible with Test Federation")
            append(", reason: '${extension.notCompatibleReason.get()}'")
        }
        logger.quiet(message)
        throw StopExecutionException(message)
    }
}

fun Test.setRuntimeProperties(
    domains: ListProperty<Domain>,
    subsets: Provider<Set<TestSubset>>,
    areNightlyTestsEnabled: Provider<Boolean>,
    extension: TestFederationExtension,
) {
    val formattedDomains = domains.formatted()
    val formattedSubsets = subsets.formatted()

    systemProperty(TEST_FEDERATION_DOMAINS_KEY, formattedDomains.get())
    environment(TEST_FEDERATION_DOMAINS_ENV_KEY, formattedDomains.get())

    systemProperty(TEST_FEDERATION_SUBSETS_KEY, formattedSubsets.get())
    environment(TEST_FEDERATION_SUBSETS_ENV_KEY, formattedSubsets.get())

    systemProperty(TEST_FEDERATION_DECLARED_SUBSETS_KEY, extension.allDeclaredSubsets.get().toArgumentString())
    environment(TEST_FEDERATION_DECLARED_SUBSETS_ENV_KEY, extension.allDeclaredSubsets.get().toArgumentString())

    systemProperty(TEST_FEDERATION_TASK_PATH_KEY, path)
    environment(TEST_FEDERATION_TASK_PATH_ENV_KEY, path)

    systemProperty(TEST_FEDERATION_NIGHTLY_KEY, areNightlyTestsEnabled.get())
    environment(TEST_FEDERATION_NIGHTLY_ENV_KEY, areNightlyTestsEnabled.get())

    extension.smokeTests.autoSamplePercentage.orNull?.let { percentage ->
        systemProperty(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_KEY, percentage)
        environment(TEST_FEDERATION_AUTO_SMOKE_TEST_PERCENTAGE_ENV_KEY, percentage)
    }
}

fun setTeamcityTags(domains: ListProperty<Domain>, subsets: Provider<Set<TestSubset>>) {
    for (domain in domains.get()) {
        println("##teamcity[addBuildTag 'Domain: $domain']")
    }
    for (testSubset in subsets.get()) {
        println("##teamcity[addBuildTag '$testSubset']")
    }
}

fun excludeNightlyTests(areNightlyTestsEnabled: Provider<Boolean>, testFramework: JUnitPlatformTestFramework) {
    if (!areNightlyTestsEnabled.get()) {
        // Exclude nightly tests if not specifically running in 'nightly' mode.
        // Like any tag filter, this also makes Gradle skip its check that the task discovers tests ('failOnNoDiscoveredTests'),
        // so that check only applies to nightly builds and local runs.
        testFramework.options.excludeTags("nightly")
    }
}

fun Test.preventVintageEngineOnClasspath() {
    // Check if classpath contains vintage engine and report it as unsupported
    if (classpath.files.any { file -> file.name.contains("junit-vintage-engine") }) {
        error("Unsupported 'junit-vintage-engine' found in classpath. Please remove this dependency")
    }
}

fun Test.configureFailingOnNonDiscoveredTests() {
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

fun ListProperty<Domain>.formatted() = map { it.toArgumentString() }
fun Provider<Set<TestSubset>>.formatted() = map { it.toArgumentString() }

