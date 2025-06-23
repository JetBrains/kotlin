import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.vintage.engine)

    testImplementation(project(":native:cli-native"))

    testImplementation(projectTests(":native:native.tests"))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

nativeTest(
    "test",
    null,
    defineJDKEnvVariables = listOf(
        JdkMajorVersion.JDK_1_8,
        JdkMajorVersion.JDK_11_0,
        JdkMajorVersion.JDK_17_0,
        JdkMajorVersion.JDK_21_0
    )
) {
    // To workaround KTI-2421, we make these tests run on JDK 11 instead of the project-default JDK 8.
    // This switches the JVM GC from ParallelGC to G1, which changes the expected performance logs in tests.
    // The performance log sanitizer replaces numbers with `$UINT$`, so `G1` becomes `G$UINT$`,
    // making the expected log funny and confusing.
    // Let's switch the GC back to ParallelGC to mitigate this:
    jvmArgs("-XX:+UseParallelGC")
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateCliTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
}