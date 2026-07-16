description = "Kotlin Assignment Compiler Plugin"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

dependencies {
    embedded(project(":kotlin-assignment-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":kotlin-assignment-compiler-plugin.cli"))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))

    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(toolsJar())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "testFixtures" { projectDefault() }
    "test" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTests {
    testData(project.isolated, "testData")

    testGenerator("org.jetbrains.kotlin.assignment.plugin.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withTestJar()

    testTask()
}
