import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
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

projectTests {
    testData(project.isolated, "testData")

    nativeTestTask(
        "test",
        requirePlatformLibs = true,
        allowParallelExecution = false, // Stress tests are resource-intensive tests and they must be run in isolation.
    ) {
        extensions.configure<TestInputsCheckExtension> {
            isNative.set(true)
            useXcode.set(OperatingSystem.current().isMacOsX)
        }
        // nativeTest sets workingDir to rootDir so here we need to override it
        workingDir = projectDir
    }
}
