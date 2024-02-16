import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.nio.file.Paths

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

testsJar()

kotlin.sourceSets.all {
    languageSettings.optIn("org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi")
}

val kotlinGradlePluginTest = project(":kotlin-gradle-plugin").sourceSets.named("test").map { it.output }

dependencies {
    testImplementation(project(":kotlin-gradle-plugin")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-common")
        }
    }
    testImplementation(project(":kotlin-allopen")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-allopen-common")
        }
    }
    testImplementation(project(":kotlin-noarg")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-noarg-common")
        }
    }
    testImplementation(project(":kotlin-lombok")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-lombok-common")
        }
    }
    testImplementation(project(":kotlin-power-assert")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-power-assert-common")
        }
    }
    testImplementation(project(":kotlin-sam-with-receiver")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-sam-with-receiver-common")
        }
    }
    testImplementation(project(":kotlin-assignment")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:kotlin-assignment-common")
        }
    }
    testImplementation(project(":atomicfu")) {
        capabilities {
            requireCapability("org.jetbrains.kotlin:atomicfu-common")
        }
    }

    testImplementation(project(":kotlin-gradle-compiler-types"))
    testImplementation(project(":kotlin-gradle-plugin-idea"))
    testImplementation(testFixtures(project(":kotlin-gradle-plugin-idea")))
    testImplementation(project(":kotlin-gradle-plugin-idea-proto"))

    testImplementation(project(":kotlin-gradle-plugin-model"))
    testImplementation(project(":kotlin-gradle-build-metrics"))
    testImplementation(project(":kotlin-tooling-metadata"))
    testImplementation(kotlinGradlePluginTest)
    testImplementation(project(":kotlin-gradle-subplugin-example"))
    testImplementation(kotlinTest("junit"))
    testImplementation(project(":kotlin-util-klib"))

    testImplementation(project(":native:kotlin-native-utils"))
    testImplementation(project(":native:kotlin-klib-commonizer-api"))

    testImplementation(project(":kotlin-compiler-embeddable"))
    testImplementation(commonDependency("org.jetbrains.intellij.deps:jdom"))
    testImplementation(project(":compiler:cli-common"))
    testImplementation(project(":compiler:build-tools:kotlin-build-statistics"))
    // testCompileOnly dependency on non-shaded artifacts is needed for IDE support
    // testRuntimeOnly on shaded artifact is needed for running tests with shaded compiler
    testCompileOnly(project(":kotlin-gradle-plugin-test-utils-embeddable"))
    testRuntimeOnly(project(":kotlin-gradle-plugin-test-utils-embeddable")) { isTransitive = false }

    testImplementation(project(path = ":examples:annotation-processor-example"))
    testImplementation(kotlinStdlib("jdk8"))
    testImplementation(project(":kotlin-parcelize-compiler"))
    testImplementation(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-serialization-json"))
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.test.host)

    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(commonDependency("com.google.code.gson:gson"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.junit.jupiter.params)

    testRuntimeOnly(project(":compiler:tests-mutes"))

    testCompileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
}

val konanDataDir: String = System.getProperty("konanDataDirForIntegrationTests")
    ?: project.rootDir
        .resolve(".kotlin")
        .resolve("konan-for-gradle-tests").absolutePath

// Aapt2 from Android Gradle Plugin 3.2 and below does not handle long paths on Windows.
val shortenTempRootName = project.providers.systemProperty("os.name").get().contains("Windows")

val splitGradleIntegrationTestTasks =
    project.providers.gradleProperty("gradle.integration.tests.split.tasks").orNull?.toBoolean()
        ?: project.kotlinBuildProperties.isTeamcityBuild

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.io.path.ExperimentalPathApi"
}

val cleanTestKitCacheTask = tasks.register<Delete>("cleanTestKitCache") {
    group = "Build"
    description = "Deletes temporary Gradle TestKit cache"

    delete(layout.buildDirectory.dir("testKitCache"))
}

tasks.register<Delete>("cleanUserHomeKonanDir") {
    description =
        "Deletes ~/.konan dir before tests. This step is necessary to ensure that no test inadvertently creates this directory during execution."

    val userHomeKonanDir = Paths.get("${System.getProperty("user.home")}/.konan")

    delete(userHomeKonanDir)

    doLast {
        logger.info("Default .konan directory user's home has been deleted: $userHomeKonanDir")
    }
}

tasks.register<Copy>("prepareNativeBundleForGradleIT") {

    description = "This task adds dependency on :kotlin-native:bundle and then copying built bundle into the tests' konan dir"

    if (project.kotlinBuildProperties.isKotlinNativeEnabled) {
        // 1. Build full Kotlin Native bundle
        dependsOn(":kotlin-native:bundle")

        // 2. Coping and extracting k/n artifacts from the 1st step to tests' konan data directory
        val (extension, unzipFunction) = when (HostManager.host) {
            KonanTarget.MINGW_X64 -> Pair("zip", ::zipTree)
            else -> Pair("tar.gz", ::tarTree)
        }

        val kotlinNativeRootDir = rootProject.findProject(":kotlin-native")?.projectDir
            ?: throw IllegalStateException("The path to kotlin-native module is undefined.")

        from(
            unzipFunction(
                kotlinNativeRootDir.resolve("kotlin-native-${HostManager.platformName()}-${project.kotlinBuildProperties.defaultSnapshotVersion}.$extension")
            )
        )
        from(
            unzipFunction(
                kotlinNativeRootDir.resolve("kotlin-native-prebuilt-${HostManager.platformName()}-${project.kotlinBuildProperties.defaultSnapshotVersion}.$extension")
            )
        )

        into(
            konanDataDir
        )

        doFirst {
            delete(konanDataDir)
        }
    }
}

fun Test.includeMppAndAndroid(include: Boolean) = includeTestsWithPattern(include) {
    addAll(listOf("*Multiplatform*", "*Mpp*", "*Android*"))
}

fun Test.includeNative(include: Boolean) = includeTestsWithPattern(include) {
    addAll(listOf("org.jetbrains.kotlin.gradle.native.*", "*Commonizer*"))
}

fun Test.applyKotlinNativeFromCurrentBranchIfNeeded() {
    val kotlinNativeFromMasterEnabled = project.kotlinBuildProperties.isKotlinNativeEnabled && project.kotlinBuildProperties.useKotlinNativeLocalDistributionForTests
    if (kotlinNativeFromMasterEnabled && !project.kotlinBuildProperties.isTeamcityBuild) {
        dependsOn(":kotlin-gradle-plugin-integration-tests:prepareNativeBundleForGradleIT")
    }

    // Providing necessary properties for running tests with k/n built from master on the local environment
    val defaultSnapshotVersion = project.kotlinBuildProperties.defaultSnapshotVersion
    if (kotlinNativeFromMasterEnabled && defaultSnapshotVersion != null) {
        systemProperty("kotlinNativeVersion", defaultSnapshotVersion)
        systemProperty("konanDataDirForIntegrationTests", konanDataDir)
    }

    // Providing necessary properties for running tests with k/n built from master on the TeamCity
    if (project.kotlinBuildProperties.isTeamcityBuild) {
        System.getProperty("kotlinNativeVersionForGradleIT")?.let {
            systemProperty("kotlinNativeVersion", it)
        }
        systemProperty("konanDataDirForIntegrationTests", konanDataDir)
    }
}

fun Test.includeTestsWithPattern(include: Boolean, patterns: (MutableSet<String>).() -> Unit) {
    if (splitGradleIntegrationTestTasks) {
        val filter = if (include)
            filter.includePatterns
        else
            filter.excludePatterns
        filter.patterns()
    }
}

fun Test.advanceGradleVersion() {
    val gradleVersionForTests = "8.5"
    systemProperty("kotlin.gradle.version.for.tests", gradleVersionForTests)
}

// additional configuration in tasks.withType<Test> below
projectTest(
    "test",
    shortenTempRootName = shortenTempRootName,
    jUnitMode = JUnitMode.JUnit5
) {
    includeMppAndAndroid(false)
    includeNative(false)
}

projectTest(
    "testAdvanceGradleVersion",
    shortenTempRootName = shortenTempRootName,
    jUnitMode = JUnitMode.JUnit5
) {
    advanceGradleVersion()
    includeMppAndAndroid(false)
    includeNative(false)
}

if (splitGradleIntegrationTestTasks) {

    projectTest(
        "testMppAndAndroid",
        shortenTempRootName = shortenTempRootName,
        jUnitMode = JUnitMode.JUnit5
    ) {
        includeMppAndAndroid(true)
    }

    projectTest(
        "testAdvanceGradleVersionMppAndAndroid",
        shortenTempRootName = shortenTempRootName,
        jUnitMode = JUnitMode.JUnit5
    ) {
        advanceGradleVersion()
        includeMppAndAndroid(true)
    }
}

val KGP_TEST_TASKS_GROUP = "Kotlin Gradle Plugin Verification"
val memoryPerGradleTestWorkerMb = 6000
val maxParallelTestForks =
    (totalMaxMemoryForTestsMb / memoryPerGradleTestWorkerMb).coerceIn(1, Runtime.getRuntime().availableProcessors())

val allParallelTestsTask = tasks.register<Test>("kgpAllParallelTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Runs all tests for Kotlin Gradle plugins except daemon ones"

    maxParallelForks = maxParallelTestForks

    useJUnitPlatform {
        excludeTags("DaemonsKGP")
        includeEngines("junit-jupiter")
    }
}

val jvmTestsTask = tasks.register<Test>("kgpJvmTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run tests for Kotlin/JVM part of Gradle plugin"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("JvmKGP")
        excludeTags("JsKGP", "NativeKGP", "DaemonsKGP", "OtherKGP", "MppKGP", "AndroidKGP")
        includeEngines("junit-jupiter")
    }
}

val jsTestsTask = tasks.register<Test>("kgpJsTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run tests for Kotlin/JS part of Gradle plugin"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("JsKGP")
        excludeTags("JvmKGP", "NativeKGP", "DaemonsKGP", "OtherKGP", "MppKGP", "AndroidKGP")
        includeEngines("junit-jupiter")
    }
}

val nativeTestsTask = tasks.register<Test>("kgpNativeTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run tests for Kotlin/Native part of Gradle plugin"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("NativeKGP")
        excludeTags("JvmKGP", "JsKGP", "DaemonsKGP", "OtherKGP", "MppKGP", "AndroidKGP")
        includeEngines("junit-jupiter")
    }
    applyKotlinNativeFromCurrentBranchIfNeeded()
}

// Daemon tests could run only sequentially as they could not be shared between parallel test builds
val daemonsTestsTask = tasks.register<Test>("kgpDaemonTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run only Gradle and Kotlin daemon tests for Kotlin Gradle Plugin"
    maxParallelForks = 1

    useJUnitPlatform {
        includeTags("DaemonsKGP")
        excludeTags("JvmKGP", "JsKGP", "NativeKGP", "OtherKGP", "MppKGP", "AndroidKGP")
        includeEngines("junit-jupiter")
    }
}

val otherPluginsTestTask = tasks.register<Test>("kgpOtherTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run tests for all support plugins, such as kapt, allopen, etc"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("OtherKGP")
        excludeTags("JvmKGP", "JsKGP", "NativeKGP", "DaemonsKGP", "MppKGP", "AndroidKGP")
        includeEngines("junit-jupiter")
    }
    applyKotlinNativeFromCurrentBranchIfNeeded()
}

val mppTestsTask = tasks.register<Test>("kgpMppTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run Multiplatform Kotlin Gradle plugin tests"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("MppKGP")
        excludeTags("JvmKGP", "JsKGP", "NativeKGP", "DaemonsKGP", "OtherKGP", "AndroidKGP")
        includeEngines("junit-jupiter")
    }
    applyKotlinNativeFromCurrentBranchIfNeeded()
}

val androidTestsTask = tasks.register<Test>("kgpAndroidTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run Android Kotlin Gradle plugin tests"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("AndroidKGP")
        excludeTags("JvmKGP", "JsKGP", "NativeKGP", "DaemonsKGP", "OtherKGP", "MppKGP")
        includeEngines("junit-jupiter")
    }
}

tasks.named<Task>("check") {
    dependsOn("testAdvanceGradleVersion")
    dependsOn(jvmTestsTask, jsTestsTask, nativeTestsTask, daemonsTestsTask, otherPluginsTestTask, mppTestsTask, androidTestsTask)
    if (splitGradleIntegrationTestTasks) {
        dependsOn("testAdvanceGradleVersionMppAndAndroid")
        dependsOn("testMppAndAndroid")
    }
}

tasks.withType<Test> {
    // Disable KONAN_DATA_DIR env variable for all integration tests
    // because we are using `konan.data.dir` gradle property instead
    environment.remove("KONAN_DATA_DIR")

    val noTestProperty = project.providers.gradleProperty("noTest")
    onlyIf { !noTestProperty.isPresent }

    dependsOn(":kotlin-gradle-plugin:validatePlugins")
    dependsOnKotlinGradlePluginInstall()
    dependsOn(":gradle:android-test-fixes:install")
    dependsOn(":gradle:gradle-warnings-detector:install")
    dependsOn(":gradle:kotlin-compiler-args-properties:install")
    dependsOn(":libraries:tools:gradle:fus-statistics-gradle-plugin:install")
    dependsOn(":examples:annotation-processor-example:install")
    dependsOn(":kotlin-dom-api-compat:install")
    if (project.kotlinBuildProperties.isTeamcityBuild) {
        dependsOn(":kotlin-gradle-plugin-integration-tests:cleanUserHomeKonanDir")
    }

    systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
    systemProperty("runnerGradleVersion", gradle.gradleVersion)

    val installCocoapods = project.findProperty("installCocoapods") as String?
    if (installCocoapods != null) {
        systemProperty("installCocoapods", installCocoapods)
    }

    val jdk8Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
    val jdk11Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_11_0)
    val jdk17Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_17_0)
    val jdk21Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_21_0)
    val mavenLocalRepo = project.providers.systemProperty("maven.repo.local").orNull

    // Query required JDKs paths only on execution phase to avoid triggering auto-download on project configuration phase
    // names should follow "jdk\\d+Home" regex where number is a major JDK version
    doFirst {
        systemProperty("jdk8Home", jdk8Provider.get())
        systemProperty("jdk11Home", jdk11Provider.get())
        systemProperty("jdk17Home", jdk17Provider.get())
        systemProperty("jdk21Home", jdk21Provider.get())
        if (mavenLocalRepo != null) {
            systemProperty("maven.repo.local", mavenLocalRepo)
        }
    }

    useAndroidSdk()

    val shouldApplyJunitPlatform = name !in setOf(
        allParallelTestsTask.name,
        jvmTestsTask.name,
        jsTestsTask.name,
        nativeTestsTask.name,
        daemonsTestsTask.name,
        otherPluginsTestTask.name,
        mppTestsTask.name,
        androidTestsTask.name
    )
    if (shouldApplyJunitPlatform) {
        maxHeapSize = "512m"
        useJUnitPlatform {
            includeEngines("junit-vintage")
        }
    }

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
                    val output =
                        "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} successes, ${result.failedTestCount} failures, ${result.skippedTestCount} skipped)"
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
