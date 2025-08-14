plugins {
    kotlin("jvm")
    id("compiler-tests-convention")
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

compilerTests {
    testData(project.isolated, "testData")
    nativeTestTask(
        "test",
        tag = null,
        allowParallelExecution = false, // some of the tests may spawn quite a lot of threads
    ) {
        // nativeTest sets workingDir to rootDir so here we need to override it
        workingDir = projectDir
    }
}
