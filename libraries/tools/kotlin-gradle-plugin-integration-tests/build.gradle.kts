import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.nio.file.Paths
import java.time.Duration

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("android-sdk-provisioner")
    id("gradle-plugin-compiler-dependency-configuration")
}

testsJar()

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi",
            "org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi",
            "kotlin.io.path.ExperimentalPathApi",
        )
    }
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
    testImplementation(intellijJDom())
    testImplementation(intellijPlatformUtil())
    testImplementation(project(":compiler:cli-common"))
    testImplementation(project(":compiler:build-tools:kotlin-build-statistics"))
    // testCompileOnly dependency on non-shaded artifacts is needed for IDE support
    // testRuntimeOnly on shaded artifact is needed for running tests with shaded compiler
    testCompileOnly(project(":kotlin-gradle-plugin-test-utils-embeddable"))
    testRuntimeOnly(project(":kotlin-gradle-plugin-test-utils-embeddable")) { isTransitive = false }

    // AGP classes for buildScriptInjection's
    testImplementation(libs.android.gradle.plugin.gradle.api) { isTransitive = false }

    testImplementation(project(path = ":examples:annotation-processor-example"))
    testImplementation(kotlinStdlib("jdk8"))
    testImplementation(project(":kotlin-parcelize-compiler"))
    testImplementation(commonDependency("org.jetbrains.intellij.deps", "trove4j"))
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.server.test.host)

    testImplementation(gradleApi())
    testImplementation(gradleTestKit())
    testImplementation(commonDependency("com.google.code.gson:gson"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.oshi.core)

    testApi(project(":compiler:tests-mutes:mutes-junit5"))

    testCompileOnly(libs.intellij.asm)
}

val konanDataDir: String = System.getProperty("konanDataDirForIntegrationTests")
    ?: project.rootDir
        .resolve(".kotlin")
        .resolve("konan-for-gradle-tests").absolutePath

tasks.register<Delete>("cleanTestKitCache") {
    group = "Build"
    description = "Deletes temporary Gradle TestKit cache"

    delete(layout.buildDirectory.dir("testKitCache"))
    delete(layout.buildDirectory.dir("kgpTestInfra"))
}

val cleanUserHomeKonanDir by tasks.registering(Delete::class) {
    description = "Only runs on CI. " +
            "Deletes ~/.konan dir before tests, to ensure that no test inadvertently creates this directory during execution."

    val isTeamCityBuild = project.kotlinBuildProperties.isTeamcityBuild
    onlyIf("Build is running on TeamCity") { isTeamCityBuild }

    val userHomeKonanDir = Paths.get("${System.getProperty("user.home")}/.konan")
    delete(userHomeKonanDir)

    doLast {
        logger.info("Default .konan directory user's home has been deleted: $userHomeKonanDir")
    }
}

val prepareNativeBundleForGradleIT by tasks.registering {
    description = "This task adds dependency on :kotlin-native:install"

    if (project.kotlinBuildProperties.isKotlinNativeEnabled) {
        // Build full Kotlin Native bundle
        dependsOn(":kotlin-native:install")
    }
}

val createProvisionedOkFiles by tasks.registering {

    description = "This task creates `provisioned.ok` file for each preconfigured k/n native bundle." +
            "Kotlin/Native bundle can be prepared in two ways:" +
            "`prepareNativeBundleForGradleIT` task for local environment and `Compiler Dist: full bundle` build for CI environment."

    mustRunAfter(prepareNativeBundleForGradleIT)

    val konanDistributions = File(konanDataDir)

    doLast {
        konanDistributions
            .walkTopDown().maxDepth(1)
            .filter { file -> file != konanDistributions }
            .filter { file -> file.isDirectory }
            .toSet()
            .forEach {
                File(it, "provisioned.ok").createNewFile()
            }
    }
}

fun Test.applyKotlinNativeFromCurrentBranchIfNeeded() {
    val kotlinNativeFromMasterEnabled =
        project.kotlinBuildProperties.isKotlinNativeEnabled && project.kotlinBuildProperties.useKotlinNativeLocalDistributionForTests

    //add native bundle dependencies for local test run
    if (kotlinNativeFromMasterEnabled && !project.kotlinBuildProperties.isTeamcityBuild) {
        dependsOn(prepareNativeBundleForGradleIT)
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
    dependsOn(createProvisionedOkFiles)
}

val KGP_TEST_TASKS_GROUP = "Kotlin Gradle Plugin Verification"

// Disabling test task as it does nothing
tasks.test {
    enabled = false
    group = null
    description = "Disabled - use KGP specific tasks in the '$KGP_TEST_TASKS_GROUP' group instead."
}

val memoryPerGradleTestWorkerMb = 6000
val maxParallelTestForks =
    (totalMaxMemoryForTestsMb / memoryPerGradleTestWorkerMb).coerceIn(1, Runtime.getRuntime().availableProcessors())

// Must be in sync with TestVersions.kt KTI-1612
val gradleVersions = listOf(
    "7.6.3",
    "8.0.2",
    "8.1.1",
    "8.2.1",
    "8.3",
    "8.4",
    "8.5",
    "8.6",
    "8.7",
    "8.8",
    "8.9",
    "8.10.2",
    "8.11.1",
)

if (project.kotlinBuildProperties.isTeamcityBuild) {
    val junitTags = listOf("JvmKGP", "DaemonsKGP", "JsKGP", "NativeKGP", "MppKGP", "AndroidKGP", "OtherKGP")
    val requiresKotlinNative = listOf("NativeKGP", "MppKGP", "OtherKGP")
    val gradleVersionTaskGroup = "Kotlin Gradle Plugin Verification grouped by Gradle version"

    junitTags.forEach { junitTag ->
        val taskPrefix = "kgp${junitTag.substringBefore("KGP")}"
        val tasksByGradleVersion = gradleVersions.map { gradleVersion ->
            tasks.register<Test>("${taskPrefix}TestsForGradle_${gradleVersion.replace(".", "_")}") {
                group = gradleVersionTaskGroup
                description = "Runs all tests for Kotlin Gradle plugins against Gradle $gradleVersion"
                maxParallelForks = maxParallelTestForks

                systemProperty("gradle.integration.tests.gradle.version.filter", gradleVersion)
                systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
                if (junitTag in requiresKotlinNative) {
                    applyKotlinNativeFromCurrentBranchIfNeeded()
                }

                useJUnitPlatform {
                    includeTags(junitTag)
                    excludeTags(*(junitTags - junitTag).toTypedArray())
                }
            }
        }

        tasks.register("${taskPrefix}TestsGroupedByGradleVersion") {
            group = gradleVersionTaskGroup
            dependsOn(tasksByGradleVersion)
        }
    }
}

tasks.register<Test>("kgpAllParallelTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Runs all tests for Kotlin Gradle plugins except daemon ones"

    maxParallelForks = maxParallelTestForks

    useJUnitPlatform {
        excludeTags("DaemonsKGP")
    }
}

val jvmTestsTask = tasks.register<Test>("kgpJvmTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run tests for Kotlin/JVM part of Gradle plugin"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("JvmKGP")
        excludeTags("JsKGP", "NativeKGP", "DaemonsKGP", "OtherKGP", "MppKGP", "AndroidKGP", "SwiftExportKGP")
    }
}

val swiftExportTestsTask = tasks.register<Test>("kgpSwiftExportTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run Swift Export Kotlin Gradle plugin tests"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("SwiftExportKGP")
        excludeTags("JvmKGP", "JsKGP", "DaemonsKGP", "OtherKGP", "MppKGP", "AndroidKGP", "NativeKGP")
    }
    applyKotlinNativeFromCurrentBranchIfNeeded()
}

val jsTestsTask = tasks.register<Test>("kgpJsTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run tests for Kotlin/JS part of Gradle plugin"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("JsKGP")
        excludeTags("JvmKGP", "NativeKGP", "DaemonsKGP", "OtherKGP", "MppKGP", "AndroidKGP", "SwiftExportKGP")
    }
}

val nativeTestsTask = tasks.register<Test>("kgpNativeTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run tests for Kotlin/Native part of Gradle plugin"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("NativeKGP")
        excludeTags("JvmKGP", "JsKGP", "DaemonsKGP", "OtherKGP", "MppKGP", "AndroidKGP", "SwiftExportKGP")
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
        excludeTags("JvmKGP", "JsKGP", "NativeKGP", "OtherKGP", "MppKGP", "AndroidKGP", "SwiftExportKGP")
    }
}

val otherPluginsTestTask = tasks.register<Test>("kgpOtherTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run tests for all support plugins, such as kapt, allopen, etc"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("OtherKGP")
        excludeTags("JvmKGP", "JsKGP", "NativeKGP", "DaemonsKGP", "MppKGP", "AndroidKGP", "SwiftExportKGP")
    }
    applyKotlinNativeFromCurrentBranchIfNeeded()
}

val mppTestsTask = tasks.register<Test>("kgpMppTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run Multiplatform Kotlin Gradle plugin tests"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("MppKGP")
        excludeTags("JvmKGP", "JsKGP", "NativeKGP", "DaemonsKGP", "OtherKGP", "AndroidKGP", "SwiftExportKGP")
    }
    applyKotlinNativeFromCurrentBranchIfNeeded()
}

val androidTestsTask = tasks.register<Test>("kgpAndroidTests") {
    group = KGP_TEST_TASKS_GROUP
    description = "Run Android Kotlin Gradle plugin tests"
    maxParallelForks = maxParallelTestForks
    useJUnitPlatform {
        includeTags("AndroidKGP")
        excludeTags("JvmKGP", "JsKGP", "NativeKGP", "DaemonsKGP", "OtherKGP", "MppKGP", "SwiftExportKGP")
    }
}

tasks.named<Task>("check") {
    dependsOn(
        jvmTestsTask,
        jsTestsTask,
        nativeTestsTask,
        daemonsTestsTask,
        otherPluginsTestTask,
        mppTestsTask,
        androidTestsTask,
        swiftExportTestsTask,
    )
}

/**
 * The JVM toolchain is configured to version 17.
 * However, that breaks buildscript injection for tests that are ran on JDK 8.
 * Such setup allows to use new Java API in the test infrastructure.
 */
fun configureJvmTarget8() {
    tasks.compileTestJava {
        sourceCompatibility = "8"
        targetCompatibility = "8"
    }

    tasks.compileTestKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_1_8
        }
    }
}

configureJvmTarget8()

tasks.withType<Test>().configureEach {
    // Disable KONAN_DATA_DIR env variable for all integration tests
    // because we are using `konan.data.dir` gradle property instead
    environment.remove("KONAN_DATA_DIR")

    val noTestProperty = project.providers.gradleProperty("noTest")
    onlyIf { !noTestProperty.isPresent }

    // Trigger task timeout earlier than TC timeout, so we could collect more info what went wrong with IT tests
    // The longest one are on MacOS/X64 agents in release configurations
    timeout.set(Duration.ofHours(7))

    dependsOn(":kotlin-gradle-plugin:validatePlugins")
    dependsOnKotlinGradlePluginInstall()
    dependsOn(":gradle:android-test-fixes:install")
    dependsOn(":gradle:gradle-warnings-detector:install")
    dependsOn(":gradle:kotlin-compiler-args-properties:install")
    dependsOn(":examples:annotation-processor-example:install")
    dependsOn(":kotlin-dom-api-compat:install")
    dependsOn(cleanUserHomeKonanDir)

    systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
    systemProperty("runnerGradleVersion", gradle.gradleVersion)
    systemProperty("composeSnapshotVersion", composeRuntimeSnapshot.versions.snapshot.version.get())
    systemProperty("composeSnapshotId", composeRuntimeSnapshot.versions.snapshot.id.get())

    val installCocoapods = project.findProperty("installCocoapods") as String?
    if (installCocoapods != null) {
        systemProperty("installCocoapods", installCocoapods)
    }

    // Gradle 8.10 requires running on at least JDK 17
    javaLauncher.value(project.getToolchainLauncherFor(JdkMajorVersion.JDK_17_0)).disallowChanges()

    val jdk8Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
    val jdk11Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_11_0)
    val jdk17Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_17_0)
    val jdk21Provider = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_21_0)
    val mavenLocalRepo = project.providers.systemProperty("maven.repo.local").orNull

    val compileTestDestination = kotlin.target
        .compilations[KotlinCompilation.TEST_COMPILATION_NAME]
        .compileTaskProvider
        .flatMap { task ->
            (task as KotlinJvmCompile).destinationDirectory
        }
    doFirst {
        systemProperty("buildGradleKtsInjectionsClasspath", compileTestDestination.get().asFile.absolutePath)
    }

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

    androidSdkProvisioner {
        provideToThisTaskAsSystemProperty(ProvisioningType.SDK)
    }

    useJUnitPlatform {
        includeEngines("junit-jupiter")
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
