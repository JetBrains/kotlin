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
) {
    // To workaround KTI-2421, we make these tests run on JDK 11 instead of the project-default JDK 8.
    // Kotlin test infra uses reflection to access JDK internals.
    // With JDK 11, some JVM args are required to silence the warnings caused by that:
    jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED")
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateKlibNativeTestsKt") {
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    dependsOn(":compiler:generateTestData")
}
