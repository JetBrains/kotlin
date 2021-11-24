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
    TEST_MODE("mode"),
    USE_CACHE("useCache"),
    EXECUTION_TIMEOUT("executionTimeout");

    private val propertyName = "kotlin.internal.native.test.$shortName"

    fun setUpFromGradleProperty(task: Test, defaultValue: () -> Any? = { null }) {
        val propertyValue = task.project.findProperty(propertyName) ?: defaultValue()
        if (propertyValue != null) task.systemProperty(propertyName, propertyValue)
    }
}

if (kotlinBuildProperties.isKotlinNativeEnabled) {
    projectTest(taskName = "test", jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":kotlin-native:dist" /*, ":kotlin-native:distPlatformLibs"*/)
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

        useJUnitPlatform()
    }
} else {
    getOrCreateTask<Test>(taskName = "test") {
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

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeBlackboxTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11))
    dependsOn(":compiler:generateTestData")
}
