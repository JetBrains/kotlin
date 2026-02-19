description = "Kotlin AllOpen Compiler Plugin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    embedded(project(":kotlin-allopen-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.cli")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.k2")) { isTransitive = false }

    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.common"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.k1"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.k2"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.cli"))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
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
    testTask(jUnitMode = JUnitMode.JUnit5)

    testGenerator("org.jetbrains.kotlin.allopen.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()

    testData(project(":kotlin-allopen-compiler-plugin").isolated, "testData")
}
