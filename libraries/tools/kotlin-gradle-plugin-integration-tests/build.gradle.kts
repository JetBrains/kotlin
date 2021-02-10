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

val kotlinGradlePluginTest = project(":kotlin-gradle-plugin").sourceSets.getByName("test")

dependencies {
    testCompile(project(":kotlin-gradle-plugin"))
    testCompile(kotlinGradlePluginTest.output)
    testCompile(project(":kotlin-gradle-subplugin-example"))
    testCompile(project(":kotlin-allopen"))
    testCompile(project(":kotlin-noarg"))
    testCompile(project(":kotlin-sam-with-receiver"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":native:kotlin-native-utils"))

    testCompile(projectRuntimeJar(":kotlin-compiler-embeddable"))
    testCompile(intellijCoreDep()) { includeJars("jdom") }
    // testCompileOnly dependency on non-shaded artifacts is needed for IDE support
    // testRuntime on shaded artifact is needed for running tests with shaded compiler
    testCompileOnly(project(path = ":kotlin-gradle-plugin-test-utils-embeddable", configuration = "compile"))
    testRuntime(projectRuntimeJar(":kotlin-gradle-plugin-test-utils-embeddable"))

    testCompile(project(path = ":examples:annotation-processor-example"))
    testCompile(kotlinStdlib("jdk8"))
    testCompile(project(":kotlin-reflect"))
    testCompile(project(":kotlin-android-extensions"))
    testCompile(project(":kotlin-parcelize-compiler"))
    testCompile(commonDep("org.jetbrains.intellij.deps", "trove4j"))

    testCompile(gradleApi())

    testRuntime(projectRuntimeJar(":kotlin-android-extensions"))
    testRuntime(project(":compiler:tests-mutes"))

    // Workaround for missing transitive import of the common(project `kotlin-test-common`
    // for `kotlin-test-jvm` into the IDE:
    testCompileOnly(project(":kotlin-test:kotlin-test-common")) { isTransitive = false }
    testCompileOnly("org.jetbrains.intellij.deps:asm-all:9.0")
}

// Aapt2 from Android Gradle Plugin 3.2 and below does not handle long paths on Windows.
val shortenTempRootName = System.getProperty("os.name")!!.contains("Windows")

val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild ||
        try {
            project.properties["gradle.integration.tests.split.tasks"]?.toString()?.toBoolean() ?: false
        } catch (_: Exception) { false }

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
    val gradleVersionForTests = "6.3"
    systemProperty("kotlin.gradle.version.for.tests", gradleVersionForTests)
}

// additional configuration in tasks.withType<Test> below
projectTest("test", shortenTempRootName = shortenTempRootName) {
    includeMppAndAndroid(false)
    includeNative(false)
}

projectTest("testAdvanceGradleVersion", shortenTempRootName = shortenTempRootName) {
    advanceGradleVersion()
    includeMppAndAndroid(false)
    includeNative(false)
}

if (isTeamcityBuild) {
    projectTest("testNative", shortenTempRootName = shortenTempRootName) {
        includeNative(true)
    }

    projectTest("testAdvanceGradleVersionNative", shortenTempRootName = shortenTempRootName) {
        advanceGradleVersion()
        includeNative(true)
    }

    projectTest("testMppAndAndroid", shortenTempRootName = shortenTempRootName) {
        includeMppAndAndroid(true)
    }

    projectTest("testAdvanceGradleVersionMppAndAndroid", shortenTempRootName = shortenTempRootName) {
        advanceGradleVersion()
        includeMppAndAndroid(true)
    }
}

tasks.named<Task>("check") {
    dependsOn("testAdvanceGradleVersion")
    if (isTeamcityBuild) {
        dependsOn("testAdvanceGradleVersionMppAndAndroid")
        dependsOn("testMppAndAndroid")
        dependsOn("testNative")
        dependsOn("testAdvanceGradleVersionNative")
    }
}

gradle.taskGraph.whenReady {
    // Validate that all dependencies "install" tasks are added to "test" dependencies
    // Test dependencies are specified as paths to avoid forcing dependency resolution
    // and also to avoid specifying evaluationDependsOn for each testCompile dependency.

    val notAddedTestTasks = hashSetOf<String>()
    val test = tasks.getByName("test")
    val testDependencies = test.dependsOn

    for (dependency in configurations.getByName("testCompile").allDependencies) {
        if (dependency !is ProjectDependency) continue

        val task = dependency.dependencyProject.tasks.findByName("install")
        if (task != null && !testDependencies.contains(task.path)) {
            notAddedTestTasks.add("\"${task.path}\"")
        }
    }

    if (!notAddedTestTasks.isEmpty()) {
        throw GradleException("Add the following tasks to ${test.path} dependencies:\n  ${notAddedTestTasks.joinToString(",\n  ")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jdkHome = rootProject.extra["JDK_18"] as String
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    onlyIf { !project.hasProperty("noTest") }

    dependsOn(":kotlin-gradle-plugin:validateTaskProperties")
    dependsOnKotlinPluginInstall()

    executable = "${rootProject.extra["JDK_18"]!!}/bin/java"

    systemProperty("kotlinVersion", rootProject.extra["kotlinVersion"] as String)
    systemProperty("runnerGradleVersion", gradle.gradleVersion)
    systemProperty("jdk9Home", rootProject.extra["JDK_9"] as String)
    systemProperty("jdk10Home", rootProject.extra["JDK_10"] as String)
    systemProperty("jdk11Home", rootProject.extra["JDK_11"] as String)

    val mavenLocalRepo = System.getProperty("maven.repo.local")
    if (mavenLocalRepo != null) {
        systemProperty("maven.repo.local", mavenLocalRepo)
    }

    useAndroidSdk()

    maxHeapSize = "512m"

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