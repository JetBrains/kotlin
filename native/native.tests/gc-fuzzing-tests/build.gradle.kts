plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures(project(":native:native.tests")))
    testImplementation(testFixtures(project(":native:native.tests:gc-fuzzing-tests:engine")))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTests {
    nativeTestTask(
        "test",
        allowParallelExecution = false, // some of the tests may spawn quite a lot of threads
    ) {
        // nativeTest sets workingDir to rootDir so here we need to override it
        workingDir = projectDir

        project.findProperty("gcfuzzing.id")?.let {
            systemProperty("gcfuzzing.id", it)
        }
        systemProperty("gcfuzzing.timelimit", project.findProperty("gcfuzzing.timelimit") ?: "1h")
    }
}
