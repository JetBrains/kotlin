description = "Kotlin NoArg Compiler Plugin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    embedded(project(":kotlin-noarg-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-noarg-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-noarg-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-noarg-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-noarg-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":kotlin-noarg-compiler-plugin.cli"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(project(":compiler:fir:plugin-utils"))

    testFixturesApi(intellijCore())
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly(toolsJar())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "testFixtures" { projectDefault() }
}

optInToExperimentalCompilerApi()

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)

    testGenerator("org.jetbrains.kotlin.noarg.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()

    testData(project.isolated, "testData")
}
