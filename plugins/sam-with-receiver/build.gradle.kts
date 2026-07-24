description = "Kotlin SamWithReceiver Compiler Plugin"

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
    embedded(project(":kotlin-sam-with-receiver-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-sam-with-receiver-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-sam-with-receiver-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":kotlin-sam-with-receiver-compiler-plugin.cli"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
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
testsJar()

projectTests {
    testTask()

    testGenerator("org.jetbrains.kotlin.samWithReceiver.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withTestJar()
    withScriptRuntime()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()

    testData(project(":kotlin-sam-with-receiver-compiler-plugin").isolated, "testData")
}
