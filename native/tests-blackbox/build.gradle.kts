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
    testImplementation(project(":kotlin-compiler-runner-unshaded"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":generators:test-generator"))
    testApiJUnit5()

    testRuntimeOnly(intellijDep()) { includeJars("trove4j", "intellij-deps-fastutil-8.4.1-4") }
}

val generationRoot = projectDir.resolve("tests-gen")
val extGenerationRoot = projectDir.resolve("ext-tests-gen")

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        java.srcDirs(generationRoot.name, extGenerationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        module.generatedSourceDirs.addAll(listOf(generationRoot, extGenerationRoot))
    }
}

if (kotlinBuildProperties.isKotlinNativeEnabled) {
    val kotlinNativeHome = project(":kotlin-native").projectDir.resolve("dist")

    val kotlinNativeCompilerClassPath: Configuration by configurations.creating
    dependencies {
        kotlinNativeCompilerClassPath(project(":kotlin-native-compiler-embeddable"))
    }

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

        systemProperty("kotlin.native.home", kotlinNativeHome.absolutePath)
        systemProperty("kotlin.internal.native.classpath", kotlinNativeCompilerClassPath.files.joinToString(";"))

        // Pass Gradle properties as JVM properties so test process can read them.
        listOf(
            "kotlin.internal.native.test.mode",
            "kotlin.internal.native.test.useCache"
        ).forEach { propertyName -> findProperty(propertyName)?.let { systemProperty(propertyName, it) } }

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

val generateOwnTests by generator("org.jetbrains.kotlin.generators.tests.GenerateNativeBlackboxTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11))
}

val generateExtTests by generator("org.jetbrains.kotlin.generators.tests.GenerateExtNativeBlackboxTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11))
    dependsOn(":compiler:generateTestData")
}

val generateTests by tasks.creating<Task> {
    dependsOn(generateOwnTests, generateExtTests)
}
