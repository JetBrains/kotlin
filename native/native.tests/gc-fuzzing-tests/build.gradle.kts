import kotlin.random.Random

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
        allowParallelExecution = false, // some tests may spawn quite a lot of threads
    ) {
        // nativeTest sets workingDir to rootDir so here we need to override it
        workingDir = projectDir

        // The test id for `executeSingle`. Use for debugging.
        project.findProperty("gcfuzzing.id")?.let {
            systemProperty("gcfuzzing.id", it)
        }
        // The timeout for a single fuzzer test. After the time is out the test will try to do one final GC a nd kill itself gracefully.
        // Should be lower than `kn.executionTimeout`.
        project.findProperty("gcfuzzing.softTimeout")?.let {
            systemProperty("gcfuzzing.softTimeout", it)
        }
        // The total duration of spinning the `simpleFuzz` task.
        // The fuzzer will generate tests one after another until this timelimit is exceeded.
        systemProperty("gcfuzzing.timelimit", project.findProperty("gcfuzzing.timelimit") ?: "1h")
        // The initial seed for `simpleFuzz` generator.
        systemProperty("gcfuzzing.seed", project.findProperty("gcfuzzing.seed") ?: Random.nextInt())
        doNotTrackState(
            "Fuzzer is randomized + certain race conditions can manifest unreproducibly even from the fixed seed"
        )
    }
}
