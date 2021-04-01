import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.pill.PillExtension

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

val kotlinGradlePluginTest = project(":kotlin-gradle-plugin").sourceSets.named("test").map { it.output }

dependencies {
    testImplementation(project(":kotlin-gradle-plugin"))
    testImplementation(project(":kotlin-tooling-metadata"))
    testImplementation(kotlinGradlePluginTest)
    testImplementation(project(":kotlin-gradle-subplugin-example"))
    testImplementation(project(":kotlin-allopen"))
    testImplementation(project(":kotlin-noarg"))
    testImplementation(project(":kotlin-lombok"))
    testImplementation(project(":kotlin-sam-with-receiver"))
    testImplementation(project(":kotlin-test:kotlin-test-jvm"))
    testImplementation(project(":native:kotlin-native-utils"))

    testImplementation(projectRuntimeJar(":kotlin-compiler-embeddable"))
    testImplementation(intellijCoreDep()) { includeJars("jdom") }
    // testCompileOnly dependency on non-shaded artifacts is needed for IDE support
    // testRuntimeOnly on shaded artifact is needed for running tests with shaded compiler
    testCompileOnly(project(path = ":kotlin-gradle-plugin-test-utils-embeddable", configuration = "compile"))
    testRuntimeOnly(projectRuntimeJar(":kotlin-gradle-plugin-test-utils-embeddable"))

    testImplementation(project(path = ":examples:annotation-processor-example"))
    testImplementation(kotlinStdlib("jdk8"))
    testImplementation(project(":kotlin-reflect"))
    testImplementation(project(":kotlin-android-extensions"))
    testImplementation(project(":kotlin-parcelize-compiler"))
    testImplementation(commonDep("org.jetbrains.intellij.deps", "trove4j"))

    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation("com.google.code.gson:gson:${rootProject.extra["versions.jar.gson"]}")
    testApiJUnit5(vintageEngine = true, jupiterParams = true)

    testRuntimeOnly(projectRuntimeJar(":kotlin-android-extensions"))
    testRuntimeOnly(project(":compiler:tests-mutes"))

    // Workaround for missing transitive import of the common(project `kotlin-test-common`
    // for `kotlin-test-jvm` into the IDE:
    testCompileOnly(project(":kotlin-test:kotlin-test-common")) { isTransitive = false }
    testCompileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }
}

// Aapt2 from Android Gradle Plugin 3.2 and below does not handle long paths on Windows.
val shortenTempRootName = project.providers.systemProperty("os.name").forUseAtConfigurationTime().get().contains("Windows")

val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild ||
        try {
            project.providers.gradleProperty("gradle.integration.tests.split.tasks").forUseAtConfigurationTime().orNull
                ?.toBoolean() ?: false
        } catch (_: Exception) { false }


val cleanTestKitCacheTask = tasks.register<Delete>("cleanTestKitCache") {
    group = "Build"
    description = "Deletes temporary Gradle TestKit cache"

    delete(project.file(".testKitDir"))
}

fun Test.includeMppAndAndroid(include: Boolean) {
    if (isTeamcityBuild) {
        val mppAndAndroidTestPatterns = listOf("*Multiplatform*", "*Mpp*", "*Android*")
        val filter = if (include)
            filter.includePatterns
        else
            filter.excludePatterns
        filter.addAll(mppAndAndroidTestPatterns)
    }
}

fun Test.includeNative(include: Boolean) {
    if (isTeamcityBuild) {
        val filter = if (include)
            filter.includePatterns
        else
            filter.excludePatterns
        filter.add("org.jetbrains.kotlin.gradle.native.*")
    }
}

fun Test.advanceGradleVersion() {
    val gradleVersionForTests = "7.0"
    systemProperty("kotlin.gradle.version.for.tests", gradleVersionForTests)
}

// additional configuration in tasks.withType<Test> below
projectTest(
    "test",
    shortenTempRootName = shortenTempRootName,
    jUnit5Enabled = true
) {
    includeMppAndAndroid(false)
    includeNative(false)
    if (isTeamcityBuild) finalizedBy(cleanTestKitCacheTask)
}

projectTest(
    "testAdvanceGradleVersion",
    shortenTempRootName = shortenTempRootName,
    jUnit5Enabled = true
) {
    advanceGradleVersion()
    includeMppAndAndroid(false)
    includeNative(false)

    if (isTeamcityBuild) finalizedBy(cleanTestKitCacheTask)
}

if (isTeamcityBuild) {
    projectTest(
        "testNative",
        shortenTempRootName = shortenTempRootName,
        jUnit5Enabled = true
    ) {
        includeNative(true)
        finalizedBy(cleanTestKitCacheTask)
    }

    projectTest(
        "testAdvanceGradleVersionNative",
        shortenTempRootName = shortenTempRootName,
        jUnit5Enabled = true
    ) {
        advanceGradleVersion()
        includeNative(true)
        finalizedBy(cleanTestKitCacheTask)
    }

    projectTest(
        "testMppAndAndroid",
        shortenTempRootName = shortenTempRootName,
        jUnit5Enabled = true
    ) {
        includeMppAndAndroid(true)
        finalizedBy(cleanTestKitCacheTask)
    }

    projectTest(
        "testAdvanceGradleVersionMppAndAndroid",
        shortenTempRootName = shortenTempRootName,
        jUnit5Enabled = true
    ) {
        advanceGradleVersion()
        includeMppAndAndroid(true)
        finalizedBy(cleanTestKitCacheTask)
    }
}

tasks.named<Task>("check") {
    dependsOn("testAdvanceGradleVersion")
    if (isTeamcityBuild) {
        dependsOn("testAdvanceGradleVersionMppAndAndroid")
        dependsOn("testMppAndAndroid")
        dependsOn("testNative")
        dependsOn("testAdvanceGradleVersionNative")
        finalizedBy(cleanTestKitCacheTask)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jdkHome = rootProject.extra["JDK_18"] as String
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    val noTestProperty = project.providers.gradleProperty("noTest")
    onlyIf { !noTestProperty.isPresent }

    dependsOn(":kotlin-gradle-plugin:validatePlugins")
    dependsOnKotlinGradlePluginInstall()

    executable = "${rootProject.extra["JDK_18"]!!}/bin/java"

    systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
    systemProperty("runnerGradleVersion", gradle.gradleVersion)
    systemProperty("jdk9Home", rootProject.extra["JDK_9"] as String)
    systemProperty("jdk10Home", rootProject.extra["JDK_10"] as String)
    systemProperty("jdk11Home", rootProject.extra["JDK_11"] as String)

    val mavenLocalRepo = project.providers.systemProperty("maven.repo.local").forUseAtConfigurationTime().orNull
    if (mavenLocalRepo != null) {
        systemProperty("maven.repo.local", mavenLocalRepo)
    }

    useAndroidSdk()

    maxHeapSize = "512m"
    useJUnitPlatform()

    testLogging {
        // set options for log level LIFECYCLE
        events("passed", "skipped", "failed", "standardOut")
        showExceptions = true
        exceptionFormat = TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true

        // set options for log level DEBUG and INFO
        debug {
            events("started", "passed", "skipped", "failed", "standardOut", "standardError")
            exceptionFormat = TestExceptionFormat.FULL
        }
        info.events = debug.events
        info.exceptionFormat = debug.exceptionFormat

        addTestListener(object : TestListener {
            override fun afterSuite(desc: TestDescriptor, result: TestResult) {
                if (desc.parent == null) { // will match the outermost suite
                    val output = "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)"
                    val startItem = "|  "
                    val endItem = "  |"
                    val repeatLength = startItem.length + output.length + endItem.length
                    println("\n" + ("-".repeat(repeatLength)) + "\n" + startItem + output + endItem + "\n" + ("-".repeat(repeatLength)))
                }
            }

            override fun beforeSuite(suite: TestDescriptor) {}
            override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
            override fun beforeTest(testDescriptor: TestDescriptor) {}
        })
    }
}

tasks.register<Test>("kgpJunit5Tests") {
    group = "Verification"
    description = "Run only JUnit 5 tests for Kotlin Gradle Plugin"
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 4).coerceAtLeast(1)

    useJUnitPlatform {
        includeTags("JUnit5")
        includeEngines("junit-jupiter")
    }
}
