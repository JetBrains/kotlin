description = "Kotlin AllOpen Compiler Plugin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
}

dependencies {
    embedded(project(":kotlin-allopen-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.cli")) { isTransitive = false }
    embedded(project(":kotlin-allopen-compiler-plugin.k2")) { isTransitive = false }

    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.common"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.k2"))
    testFixturesImplementation(project(":kotlin-allopen-compiler-plugin.cli"))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:jvm.tests")))

    testRuntimeOnly(project(":analysis:analysis-api-fir"))
    testRuntimeOnly(project(":analysis:analysis-api-standalone:analysis-api-standalone-fir"))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))

    testFixturesApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
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
    testTask(defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_17_0))

    testGenerator("org.jetbrains.kotlin.allopen.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withTestJar()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()

    testData(project(":kotlin-allopen-compiler-plugin").isolated, "testData")
}
