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
    allowParallelExecution = false, // Driver tests run Native compiler from CLI. This is resource-intensive and should be done isolated.
)
