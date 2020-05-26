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
    testCompile(commonDep("org.jetbrains.intellij.deps", "trove4j"))

    testCompile(gradleApi())

    testRuntime(projectRuntimeJar(":kotlin-android-extensions"))

    // Workaround for missing transitive import of the common(project `kotlin-test-common`
    // for `kotlin-test-jvm` into the IDE:
    testCompileOnly(project(":kotlin-test:kotlin-test-common")) { isTransitive = false }
}

// Aapt2 from Android Gradle Plugin 3.2 and below does not handle long paths on Windows.
val shortenTempRootName = System.getProperty("os.name")!!.contains("Windows")

// additional configuration in tasks.withType<Test> below
projectTest("test", shortenTempRootName = shortenTempRootName) {}

projectTest("testAdvanceGradleVersion", shortenTempRootName = shortenTempRootName) {
    val gradleVersionForTests = "5.3-rc-2"
    systemProperty("kotlin.gradle.version.for.tests", gradleVersionForTests)
}

tasks.named<Task>("check") {
    dependsOn("testAdvanceGradleVersion")
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
    dependsOn(
        ":kotlin-allopen:install",
        ":kotlin-allopen:plugin-marker:install",
        ":kotlin-noarg:install",
        ":kotlin-allopen:plugin-marker:install",
        ":kotlin-sam-with-receiver:install",
        ":kotlin-android-extensions:install",
        ":kotlin-build-common:install",
        ":kotlin-compiler-embeddable:install",
        ":native:kotlin-native-utils:install",
        ":kotlin-util-klib:install",
        ":kotlin-util-io:install",
        ":kotlin-compiler-runner:install",
        ":kotlin-daemon-embeddable:install",
        ":kotlin-daemon-client:install",
        ":kotlin-gradle-plugin-api:install",
        ":kotlin-gradle-plugin:install",
        ":kotlin-gradle-plugin-model:install",
        ":kotlin-gradle-plugin:plugin-marker:install",
        ":kotlin-reflect:install",
        ":kotlin-annotation-processing-gradle:install",
        ":kotlin-test:kotlin-test-common:install",
        ":kotlin-test:kotlin-test-annotations-common:install",
        ":kotlin-test:kotlin-test-jvm:install",
        ":kotlin-test:kotlin-test-js:install",
        ":kotlin-gradle-subplugin-example:install",
        ":kotlin-stdlib-common:install",
        ":kotlin-stdlib:install",
        ":kotlin-stdlib-jdk8:install",
        ":kotlin-stdlib-js:install",
        ":examples:annotation-processor-example:install",
        ":kotlin-script-runtime:install",
        ":kotlin-scripting-common:install",
        ":kotlin-scripting-jvm:install",
        ":kotlin-scripting-compiler:install",
        ":kotlin-scripting-compiler-impl:install",
        ":kotlin-test-js-runner:install",
        ":kotlin-source-map-loader:install"
    )

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