plugins {
    kotlin("jvm")
    id("jps-compatible")
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
    testRuntimeOnly(projectTests(":analysis:low-level-api-fir"))
    testRuntimeOnly(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:analysis-api-fir"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
}

sourceSets {
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

val test by nativeTest("test", null, requirePlatformLibs = true)

testsJar()
