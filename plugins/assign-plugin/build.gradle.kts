description = "Kotlin Assignment Compiler Plugin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    embedded(project(":kotlin-assignment-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-assignment-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":compiler:backend"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":kotlin-assignment-compiler-plugin.cli"))
    testFixturesImplementation(project(":kotlin-scripting-jvm-host-unshaded"))

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))

    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testFixturesImplementation(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(toolsJar())

    testFixturesApi(intellijCore())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "testFixtures" { projectDefault() }
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

    testTask(jUnitMode = JUnitMode.JUnit5)
}
