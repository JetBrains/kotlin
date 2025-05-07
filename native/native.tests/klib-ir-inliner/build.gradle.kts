plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(project(":compiler:ir.objcinterop"))
    testImplementation(project(":compiler:ir.backend.native"))
    testImplementation(project(":compiler:ir.serialization.native"))
    testImplementation(project(":compiler:test-infrastructure"))
    testImplementation(project(":kotlin-util-klib-abi"))
    testImplementation(projectTests(":native:native.tests"))
    testImplementation(projectTests(":kotlin-util-klib-abi"))
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
    allowParallelExecution = true,
)

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateKlibNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}
