plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

description = "A set of integration tests for Swift Export Standalone with Coroutines as a dependency"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testImplementation(testFixtures(project(":native:swift:swift-export-standalone-integration-tests")))
    testRuntimeOnly(testFixtures(project(":analysis:low-level-api-fir")))
    testRuntimeOnly(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-api-fir")))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))

    testFixturesImplementation(testFixtures(project(":native:swift:swift-export-standalone-integration-tests")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
}

sourceSets {
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(isolated, "testData")
    testData(rootProject.isolated, "native/native.tests/testData/framework")

    nativeTestTaskWithExternalDependencies(
        "test",
        requirePlatformLibs = true,
        allowUnsafe = true, // KT-85212
    ) {
        dependsOn(":kotlin-native:distInvalidateStaleCaches")
    }

    testGenerator(
        "org.jetbrains.kotlin.swiftexport.standalone.test.coroutines.TestGeneratorKt",
        generateTestsInBuildDirectory = true,
    )
}
