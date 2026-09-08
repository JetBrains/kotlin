plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesImplementation(testFixtures(project(":native:native.tests")))
    testImplementation(testFixtures(project(":native:native.tests")))
}

sourceSets {
    "main" { none() }
    "testFixtures" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTests {
    testData(project.isolated, "testData")
    nativeTestTask(
        "test",
        allowParallelExecution = false, // some of the tests may spawn quite a lot of threads
    )
}
