description = "Kotlin SamWithReceiver Compiler Plugin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
}

dependencies {
    embedded(project(":kotlin-sam-with-receiver-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-sam-with-receiver-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-sam-with-receiver-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":kotlin-sam-with-receiver-compiler-plugin.cli"))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))

    testRuntimeOnly(project(":analysis:analysis-api-fir"))
    testRuntimeOnly(project(":analysis:analysis-api-standalone:analysis-api-standalone-fir"))

    testFixturesApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:jvm.tests")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(toolsJar())
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

projectTests {
    testTask()

    testGenerator("org.jetbrains.kotlin.samWithReceiver.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withTestJar()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()

    testData(project(":kotlin-sam-with-receiver-compiler-plugin").isolated, "testData")
}
