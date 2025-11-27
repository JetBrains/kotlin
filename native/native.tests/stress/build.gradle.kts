plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures(project(":native:native.tests")))
    testFixturesImplementation(testFixtures(project(":native:native.tests")))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

testsJar {}

projectTests {
    testData(project.isolated, "testData")

    nativeTestTask(
        "test",
        requirePlatformLibs = true,
        allowParallelExecution = false, // Stress tests are resource-intensive tests and they must be run in isolation.
    ) {
        extensions.configure<TestInputsCheckExtension> {
            isNative.set(true)
        }
        // nativeTest sets workingDir to rootDir so here we need to override it
        workingDir = projectDir
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateNativeStressTestsKt", generateTestsInBuildDirectory = true) {
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }
}
