plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("java-test-fixtures")
    id("test-inputs-check-v2")
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
