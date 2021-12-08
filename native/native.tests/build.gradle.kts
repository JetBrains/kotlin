import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_11)

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(project(":kotlin-reflect"))
    testImplementation(intellijCoreDep()) { includeJars("intellij-core") }
    testImplementation(intellijPluginDep("java"))
    testImplementation(intellijDep()) { includeJars("commons-lang-2.4", "serviceMessages", rootProject = rootProject) }
    testImplementation(project(":kotlin-compiler-runner-unshaded"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":generators:test-generator"))
    testApiJUnit5()

    testRuntimeOnly(intellijDep()) { includeJars("trove4j", "intellij-deps-fastutil-8.4.1-4") }
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
    TEST_MODE("mode"),
    USE_CACHE("useCache"),
    EXECUTION_TIMEOUT("executionTimeout");

    private val propertyName = "kotlin.internal.native.test.$shortName"

    fun setUpFromGradleProperty(task: Test, defaultValue: () -> Any? = { null }) {
        val propertyValue = task.project.findProperty(propertyName) ?: defaultValue()
        if (propertyValue != null) task.systemProperty(propertyName, propertyValue)
    }
}

fun Test.setUpBlackBoxTest(tag: String) {
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        dependsOn(":kotlin-native:dist")
        workingDir = rootDir

        maxHeapSize = "6G" // Extra heap space for Kotlin/Native compiler.
        jvmArgs("-XX:MaxJavaStackTraceDepth=1000000") // Effectively remove the limit for the amount of stack trace elements in Throwable.

        // Double the stack size. This is needed to compile some marginal tests with extra-deep IR tree, which requires a lot of stack frames
        // for visiting it. Example: codegen/box/strings/concatDynamicWithConstants.kt
        // Such tests are successfully compiled in old test infra with the default 1 MB stack just by accident. New test infra requires ~55
        // additional stack frames more compared to the old one because of another launcher, etc. and it turns out this is not enough.
        jvmArgs("-Xss2m")

        TestProperty.KOTLIN_NATIVE_HOME.setUpFromGradleProperty(this) {
            project(":kotlin-native").projectDir.resolve("dist").absolutePath
        }

        TestProperty.COMPILER_CLASSPATH.setUpFromGradleProperty(this) {
            configurations.detachedConfiguration(dependencies.project(":kotlin-native-compiler-embeddable")).files.joinToString(";")
        }

        // Pass Gradle properties as JVM properties so test process can read them.
        TestProperty.TEST_MODE.setUpFromGradleProperty(this)
        TestProperty.USE_CACHE.setUpFromGradleProperty(this)
        TestProperty.EXECUTION_TIMEOUT.setUpFromGradleProperty(this)

        ignoreFailures = true // Don't fail Gradle task if there are failed tests. Let the subsequent tasks to run as well.

        useJUnitPlatform {
            includeTags(tag)
        }
    } else {
        doFirst {
            throw GradleException(
                """
                    Can't run Kotlin/Native tests. The Kotlin/Native part of the project is currently disabled.
                    Make sure that "kotlin.native.enabled" is set to "true" in local.properties file,
                    or is passed as a Gradle command-line parameter via "-Pkotlin.native.enabled=true".
                """.trimIndent()
            )
        }
    }
}

val infrastructureTest by projectTest(taskName = "infrastructureTest", jUnitMode = JUnitMode.JUnit5) {
    setUpBlackBoxTest("infrastructure")
}

val dailyTest by projectTest(taskName = "dailyTest", jUnitMode = JUnitMode.JUnit5) {
    setUpBlackBoxTest("daily")
}

// Just an alias for daily test task.
val test: Task by tasks.getting {
    dependsOn(dailyTest)
}

projectTest(taskName = "fullTest", jUnitMode = JUnitMode.JUnit5) {
    dependsOn(dailyTest, infrastructureTest)
    // TODO: migrate and attach K/N blackbox tests from kotlin-native/backend.native/tests
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeBlackboxTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11))
    dependsOn(":compiler:generateTestData")
}
