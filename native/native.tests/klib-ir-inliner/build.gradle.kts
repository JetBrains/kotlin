plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(project(":compiler:ir.objcinterop"))
    testFixturesApi(project(":compiler:ir.backend.native"))
    testFixturesApi(project(":compiler:ir.serialization.native"))
    testFixturesApi(project(":compiler:test-infrastructure"))
    testFixturesApi(project(":kotlin-util-klib-abi"))
    testFixturesApi(testFixtures(project(":native:native.tests")))
    testFixturesApi(testFixtures(project(":kotlin-util-klib-abi")))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
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
