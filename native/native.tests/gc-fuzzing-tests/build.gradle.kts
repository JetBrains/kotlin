import kotlin.random.Random

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check-v2")
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
        // If set, execute a single test with the given id instead of the fuzzing process.
        project.providers.gradleProperty("gcfuzzing.single.id").orNull?.let {
            systemProperty("gcfuzzing.single.id", it)
        }
        // The timeout for a single fuzzer test. After the time is out the test will try to do one final GC a nd kill itself gracefully.
        // Should be lower than `kn.executionTimeout`.
        project.providers.gradleProperty("gcfuzzing.softTimeout").orNull?.let {
            systemProperty("gcfuzzing.softTimeout", it)
        }
        // The total duration of spinning the `simpleFuzz` task.
        // The fuzzer will generate tests one after another until this timelimit is exceeded.
        systemProperty("gcfuzzing.timelimit", project.providers.gradleProperty("gcfuzzing.timelimit").getOrElse("1h"))
        // The initial seed for `simpleFuzz` generator.
        systemProperty("gcfuzzing.seed", project.providers.gradleProperty("gcfuzzing.seed").getOrElse(Random.nextInt().toString()))
        doNotTrackState(
            "Fuzzer is randomized + certain race conditions can manifest unreproducibly even from the fixed seed"
        )
    }
}
