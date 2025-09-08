plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures(project(":native:native.tests")))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

projectTests {
    val testTags = findProperty("kotlin.native.tests.tags")?.toString()
    nativeTestTask(
        "test",
        testTags,
        allowParallelExecution = false, // Driver tests run Native compiler from CLI. This is resource-intensive and should be done isolated.
    )
}
