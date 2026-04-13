plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
}

description = "A set of integration tests for Swift Export Standalone"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(project(":native:swift:swift-export-standalone-integration-tests"))
    testRuntimeOnly(testFixtures(project(":analysis:low-level-api-fir")))
    testRuntimeOnly(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-api-fir")))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
}

sourceSets {
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTests {
    testData(isolated, "testData")

    nativeTestTask(
        "test",
        requirePlatformLibs = true,
        allowUnsafe = true, // KT-85212
    ) {
        dependsOn(":kotlin-native:distInvalidateStaleCaches")
        extensions.configure<TestInputsCheckExtension>("testInputsCheck") {
            allowFlightRecorder.set(true)
        }
    }
}

testsJar()
