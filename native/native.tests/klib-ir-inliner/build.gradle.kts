plugins {
    kotlin("jvm")
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
    allowParallelExecution = true,
)