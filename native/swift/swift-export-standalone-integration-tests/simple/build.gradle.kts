plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("project-tests-convention")
}

description = "A set of integration tests for Swift Export Standalone"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    testApi(platform(libs.junit.bom))
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
    nativeTestTask("test", requirePlatformLibs = true) {
        dependsOn(":kotlin-native:distInvalidateStaleCaches")
    }
}

testsJar()
