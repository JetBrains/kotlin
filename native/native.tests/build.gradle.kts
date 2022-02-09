import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_11)

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(project(":kotlin-reflect"))
    testImplementation(intellijCore())
    testImplementation(commonDependency("commons-lang:commons-lang"))
    testImplementation(commonDependency("org.jetbrains.teamcity:serviceMessages"))
    testImplementation(project(":kotlin-compiler-runner-unshaded"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":generators:test-generator"))
    testApiJUnit5()

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        java.srcDirs(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        module.generatedSourceDirs.addAll(listOf(generationRoot))
    }
}

enum class TestProperty(shortName: String) {
    // Use a separate Gradle property to pass Kotlin/Native home to tests: "kotlin.internal.native.test.nativeHome".
    // Don't use "kotlin.native.home" and similar properties for this purpose, as these properties may have undesired
    // effect on other Gradle tasks (ex: :kotlin-native:dist) that might be executed along with test task.
    KOTLIN_NATIVE_HOME("nativeHome"),
    COMPILER_CLASSPATH("compilerClasspath"),
    TEST_TARGET("target"),
    TEST_MODE("mode"),
    OPTIMIZATION_MODE("optimizationMode"),
    MEMORY_MODEL("memoryModel"),
    USE_THREAD_STATE_CHECKER("useThreadStateChecker"),
    GC_TYPE("gcType"),
    GC_SCHEDULER("gcScheduler"),
    CACHE_MODE("cacheMode"),
    EXECUTION_TIMEOUT("executionTimeout");

    private val propertyName = "kotlin.internal.native.test.$shortName"

    fun setUpFromGradleProperty(task: Test, defaultValue: () -> String? = { null }) {
        val propertyValue = readGradleProperty(task) ?: defaultValue()
        if (propertyValue != null) task.systemProperty(propertyName, propertyValue)
    }

    fun readGradleProperty(task: Test): String? = task.project.findProperty(propertyName)?.toString()
}

fun nativeTest(taskName: String, vararg tags: String) = projectTest(taskName, jUnitMode = JUnitMode.JUnit5) {
    group = "verification"

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        workingDir = rootDir
        outputs.upToDateWhen {
            // Don't treat any test task as up-to-date, no matter what.
            // Note: this project should contain only test tasks, including ones that build binaries, and ones that run binaries.
            false
        }

        maxHeapSize = "6G" // Extra heap space for Kotlin/Native compiler.
        jvmArgs("-XX:MaxJavaStackTraceDepth=1000000") // Effectively remove the limit for the amount of stack trace elements in Throwable.

        // Double the stack size. This is needed to compile some marginal tests with extra-deep IR tree, which requires a lot of stack frames
        // for visiting it. Example: codegen/box/strings/concatDynamicWithConstants.kt
        // Such tests are successfully compiled in old test infra with the default 1 MB stack just by accident. New test infra requires ~55
        // additional stack frames more compared to the old one because of another launcher, etc. and it turns out this is not enough.
        jvmArgs("-Xss2m")

        val availableCpuCores: Int = Runtime.getRuntime().availableProcessors()
        if (!kotlinBuildProperties.isTeamcityBuild
            && minOf(kotlinBuildProperties.junit5NumberOfThreadsForParallelExecution ?: 16, availableCpuCores) > 4
        ) {
            logger.info("$path JIT C2 compiler has been disabled")
            jvmArgs("-XX:TieredStopAtLevel=1") // Disable C2 if there are more than 4 CPUs at the host machine.
        }

        TestProperty.KOTLIN_NATIVE_HOME.setUpFromGradleProperty(this) {
            val testTarget = TestProperty.TEST_TARGET.readGradleProperty(this)
            dependsOn(if (testTarget != null) ":kotlin-native:${testTarget}CrossDist" else ":kotlin-native:dist")
            project(":kotlin-native").projectDir.resolve("dist").absolutePath
        }

        TestProperty.COMPILER_CLASSPATH.setUpFromGradleProperty(this) {
            val customNativeHome = TestProperty.KOTLIN_NATIVE_HOME.readGradleProperty(this)
            if (customNativeHome != null) {
                file(customNativeHome).resolve("konan/lib/kotlin-native-compiler-embeddable.jar").absolutePath
            } else {
                val kotlinNativeCompilerEmbeddable = configurations.detachedConfiguration(dependencies.project(":kotlin-native-compiler-embeddable"))
                dependsOn(kotlinNativeCompilerEmbeddable)
                kotlinNativeCompilerEmbeddable.files.joinToString(";")
            }
        }

        // Pass Gradle properties as JVM properties so test process can read them.
        TestProperty.TEST_TARGET.setUpFromGradleProperty(this)
        TestProperty.TEST_MODE.setUpFromGradleProperty(this)
        TestProperty.OPTIMIZATION_MODE.setUpFromGradleProperty(this)
        TestProperty.MEMORY_MODEL.setUpFromGradleProperty(this)
        TestProperty.USE_THREAD_STATE_CHECKER.setUpFromGradleProperty(this)
        TestProperty.GC_TYPE.setUpFromGradleProperty(this)
        TestProperty.GC_SCHEDULER.setUpFromGradleProperty(this)
        TestProperty.CACHE_MODE.setUpFromGradleProperty(this)
        TestProperty.EXECUTION_TIMEOUT.setUpFromGradleProperty(this)

        useJUnitPlatform {
            includeTags(*tags)
        }

        logger.info(
            buildString {
                appendLine("$path parallel test execution parameters:")
                append("  Available CPU cores = $availableCpuCores")
                systemProperties.filterKeys { it.startsWith("junit.jupiter") }.toSortedMap().forEach { (key, value) ->
                    append("\n  $key = $value")
                }
            }
        )
    } else
        doFirst {
            throw GradleException(
                """
                    Can't run task $path. The Kotlin/Native part of the project is currently disabled.
                    Make sure that "kotlin.native.enabled" is set to "true" in local.properties file, or is passed
                    as a Gradle command-line parameter via "-Pkotlin.native.enabled=true".
                """.trimIndent()
            )
        }
}

// Tasks that run different sorts of tests. Most frequent use case: running specific tests at TeamCity.
val infrastructureTest = nativeTest("infrastructureTest", "infrastructure")
val codegenBoxTest = nativeTest("codegenBoxTest", "codegen")
val stdlibTest = nativeTest("stdlibTest", "stdlib")
val kotlinTestLibraryTest = nativeTest("kotlinTestLibraryTest", "kotlin-test")
val klibAbiTest = nativeTest("klibAbiTest", "klib")

// "test" task is created by convention. We can't just remove it. Let's enable it in developer's environment, so it can be used
// to run any test from IDE or from console, but disable it at TeamCity where it is not supposed to be ever used.
val test by nativeTest("test" /* no tags */).apply {
    if (kotlinBuildProperties.isTeamcityBuild) {
        configure { doFirst { throw GradleException("Task $path is not supposed to be executed in TeamCity environment") } }
    }
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11))
    dependsOn(":compiler:generateTestData")
}
