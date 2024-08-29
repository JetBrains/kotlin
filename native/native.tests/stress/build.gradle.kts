plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

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
    requirePlatformLibs = true,
    allowParallelExecution = false, // Stress tests are resource-intensive tests and they must be run in isolation.
)
