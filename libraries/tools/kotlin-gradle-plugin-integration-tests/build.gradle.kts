import JdkMajorVersion.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.build.androidsdkprovisioner.ProvisioningType
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.nio.file.Paths

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("android-sdk-provisioner")
}

testsJar()

kotlin {
    compilerOptions {
        optIn.addAll(
            "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi",
            "org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi",
            "kotlin.io.path.ExperimentalPathApi",
        )
    }
}

val kotlinGradlePluginTest: Provider<SourceSetOutput> =
    project(":kotlin-gradle-plugin").sourceSets.named("test").map { it.output }

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

    testCompileOnly(libs.intellij.asm)
}

val konanDataDir: Provider<Directory> =
    layout.projectDirectory
        .dir(providers.systemProperty("konanDataDirForIntegrationTests"))
        .orElse(rootProject.layout.projectDirectory.dir(".kotlin/konan-for-gradle-tests"))

tasks.register<Delete>("cleanTestKitCache") {
    group = "Build"
    description = "Deletes temporary Gradle TestKit cache"

    delete(layout.buildDirectory.dir("testKitCache"))
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

    val isKotlinNativeEnabled = project.kotlinBuildProperties.isKotlinNativeEnabled
    onlyIf("Kotlin Native is enabled") { isKotlinNativeEnabled }

    val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild
    onlyIf("not running on TeamCity") { !isTeamcityBuild }

    val useKotlinNativeLocalDistForTests = project.kotlinBuildProperties.useKotlinNativeLocalDistributionForTests
    onlyIf("useKotlinNativeLocalDistributionForTests is enabled") { useKotlinNativeLocalDistForTests }

    // Build full Kotlin Native bundle
    dependsOn(":kotlin-native:install")
}

val createProvisionedOkFiles by tasks.registering {
    description = "This task creates `provisioned.ok` file for each preconfigured k/n native bundle." +
            "Kotlin/Native bundle can be prepared in two ways:" +
            "`prepareNativeBundleForGradleIT` task for local environment and `Compiler Dist: full bundle` build for CI environment."

    mustRunAfter(prepareNativeBundleForGradleIT)

    val konanDataDir = konanDataDir
    outputs.dir(konanDataDir).withPropertyName("konanDataDir")

    doLast {
        konanDataDir.get().asFile
            .walkTopDown().maxDepth(1)
            .filter { file -> file != konanDataDir }
            .filter { file -> file.isDirectory }
            .forEach {
                File(it, "provisioned.ok").createNewFile()
            }
    }
}

abstract class KgpNativeTestJvmArgs : CommandLineArgumentProvider {

    @get:Input
    abstract val kotlinNativeFromMasterEnabled: Property<Boolean>

    @get:Input
    abstract val isTeamcityBuild: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val kotlinNativeVersion: Property<String>

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(RELATIVE)
    abstract val konanDataDir: DirectoryProperty

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(RELATIVE)
    abstract val konanDataDirForIntegrationTests: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val kotlinNativeVersionForGradleIT: Property<String>

    override fun asArguments(): Iterable<String> = buildList {
        fun sysProp(name: String, value: String) {
            add("-D$name=$value")
        }

        if (kotlinNativeFromMasterEnabled.get() && kotlinNativeVersion.isPresent) {
            sysProp("kotlinNativeVersion", kotlinNativeVersion.get())
            sysProp("konanDataDirForIntegrationTests", konanDataDir.get().asFile.absoluteFile.invariantSeparatorsPath)
        }

        if (isTeamcityBuild.get()) {
            kotlinNativeVersionForGradleIT.orNull?.let { sysProp("kotlinNativeVersion", it) }
            sysProp("konanDataDirForIntegrationTests", konanDataDir.get().asFile.absoluteFile.invariantSeparatorsPath)
        }
    }
}

fun Test.applyKotlinNativeFromCurrentBranchIfNeeded() {
    val konanDataDir = konanDataDir
    val kotlinNativeFromMasterEnabled =
        project.kotlinBuildProperties.isKotlinNativeEnabled && project.kotlinBuildProperties.useKotlinNativeLocalDistributionForTests
    val defaultSnapshotVersion = project.kotlinBuildProperties.defaultSnapshotVersion

    jvmArgumentProviders.add(objects.newInstance(KgpNativeTestJvmArgs::class).apply {
        this.kotlinNativeFromMasterEnabled = kotlinNativeFromMasterEnabled

        // Providing necessary properties for running tests with k/n built from master on the local environment
        this.kotlinNativeVersion = defaultSnapshotVersion
        this.konanDataDir = konanDataDir

        // Providing necessary properties for running tests with k/n built from master on the TeamCity
        this.kotlinNativeVersionForGradleIT = providers.systemProperty("kotlinNativeVersionForGradleIT")
    })
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
    "8.10",
)

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

            enabled = project.kotlinBuildProperties.isTeamcityBuild

            inputs.property("gradleVersion", gradleVersion)
            systemProperty("gradle.integration.tests.gradle.version.filter", gradleVersion)
            systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")

            if (junitTag in requiresKotlinNative) {
                applyKotlinNativeFromCurrentBranchIfNeeded()
            }

            useJUnitPlatform {
                includeTags(junitTag)
                excludeTags.addAll(junitTags - junitTag)
            }
        }
    }

    tasks.register("${taskPrefix}TestsGroupedByGradleVersion") {
        group = gradleVersionTaskGroup
        dependsOn(tasksByGradleVersion)

        enabled = project.kotlinBuildProperties.isTeamcityBuild
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

tasks.check {
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

abstract class KgpTestJvmArgs @Inject constructor(
    private val javaToolchainService: JavaToolchainService,
) : CommandLineArgumentProvider {

    @get:Input
    abstract val kotlinVersion: Property<String>

    @get:Input
    abstract val runnerGradleVersion: Property<String>

    @get:Input
    abstract val composeSnapshotVersion: Property<String>

    @get:Input
    abstract val composeSnapshotId: Property<String>

    @get:Input
    @get:Optional
    abstract val installCocoapods: Property<String>

    @get:Input
    @get:Optional
    abstract val mavenRepoLocal: Property<String>

    @get:Nested
    val jdk8Launcher: Provider<JavaLauncher> = javaLauncherFor(JDK_1_8)

    @get:Nested
    val jdk11Launcher: Provider<JavaLauncher> = javaLauncherFor(JDK_11_0)

    @get:Nested
    val jdk17Launcher: Provider<JavaLauncher> = javaLauncherFor(JDK_17_0)

    @get:Nested
    val jdk21Launcher: Provider<JavaLauncher> = javaLauncherFor(JDK_21_0)

    @get:InputDirectory
    @get:PathSensitive(RELATIVE)
    abstract val compileTestKotlinOutputDir: DirectoryProperty

    override fun asArguments(): Iterable<String> = buildList {
        fun sysProp(name: String, value: String) {
            add("-D$name=$value")
        }

        sysProp("kotlinVersion", kotlinVersion.get())
        sysProp("runnerGradleVersion", runnerGradleVersion.get())
        sysProp("composeSnapshotVersion", composeSnapshotVersion.get())
        sysProp("composeSnapshotId", composeSnapshotId.get())
        installCocoapods.orNull?.let { sysProp("installCocoapods", it) }
        mavenRepoLocal.orNull?.let { sysProp("maven.repo.local", it) }

        // Query required JDKs paths only on execution phase to avoid triggering auto-download on project configuration phase.
        // Names should follow "jdk\\d+Home" regex where number is a major JDK version.
        sysProp("jdk8Home", jdk8Launcher.getInstallationPath())
        sysProp("jdk11Home", jdk11Launcher.getInstallationPath())
        sysProp("jdk17Home", jdk17Launcher.getInstallationPath())
        sysProp("jdk21Home", jdk21Launcher.getInstallationPath())

        sysProp("compileTestKotlinOutputDir", compileTestKotlinOutputDir.get().asFile.absoluteFile.invariantSeparatorsPath)
    }

    private fun javaLauncherFor(version: JdkMajorVersion): Provider<JavaLauncher> =
        javaToolchainService.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(version.majorVersion))
        }

    companion object {
        private fun Provider<JavaLauncher>.getInstallationPath(): String =
            get().metadata.installationPath.asFile.absoluteFile.invariantSeparatorsPath
    }
}

tasks.withType<Test>().configureEach {
    // Disable KONAN_DATA_DIR env variable for all integration tests
    // because we are using `konan.data.dir` gradle property instead
    environment.remove("KONAN_DATA_DIR")

    val noTestProperty = project.providers.gradleProperty("noTest")
    onlyIf("tests are not disabled by 'noTest' property") { !noTestProperty.isPresent }

    dependsOn(":kotlin-gradle-plugin:validatePlugins")
    dependsOnKotlinGradlePluginInstall()
    dependsOn(":gradle:android-test-fixes:install")
    dependsOn(":gradle:gradle-warnings-detector:install")
    dependsOn(":gradle:kotlin-compiler-args-properties:install")
    dependsOn(":examples:annotation-processor-example:install")
    dependsOn(":kotlin-dom-api-compat:install")
    dependsOn(cleanUserHomeKonanDir)

    val testCompileOutputDir = kotlin.target
        .compilations[KotlinCompilation.TEST_COMPILATION_NAME]
        .compileTaskProvider.flatMap { task ->
            (task as KotlinJvmCompile).destinationDirectory
        }

    dependsOn(tasks.compileTestKotlin)

    jvmArgumentProviders.add(
        objects.newInstance<KgpTestJvmArgs>().apply {
            this.kotlinVersion = provider { rootProject.extra["kotlinVersion"] as String }
            this.runnerGradleVersion = gradle.gradleVersion
            this.composeSnapshotVersion = composeRuntimeSnapshot.versions.snapshot.version
            this.composeSnapshotId = composeRuntimeSnapshot.versions.snapshot.id
            this.installCocoapods = provider { project.findProperty("installCocoapods") as String? }
            this.mavenRepoLocal = project.providers.systemProperty("maven.repo.local")
            this.compileTestKotlinOutputDir = testCompileOutputDir
        }
    )

    // Gradle 8.10 requires running on at least JDK 17
    javaLauncher.value(project.getToolchainLauncherFor(JDK_17_0)).disallowChanges()

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
