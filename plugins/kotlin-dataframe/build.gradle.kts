plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

val dataframeRuntimeClasspath by configurations.creating

dependencies {
    embedded(project(":kotlin-dataframe-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-dataframe-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-dataframe-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-dataframe-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(project(":kotlin-dataframe-compiler-plugin.cli"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests")))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesApi(project(":compiler:fir:plugin-utils"))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    dataframeRuntimeClasspath(libs.dataframe.core.dev)
    dataframeRuntimeClasspath(libs.dataframe.csv.dev)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(isolated, "testData")
    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()

    testTask(jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_17_0, JdkMajorVersion.JDK_1_8)) {
        extensions.configure<TestInputsCheckExtension>("testInputsCheck") {
            allowFlightRecorder.set(true)
        }
        addClasspathProperty(dataframeRuntimeClasspath, "kotlin.dataframe.plugin.test.classpath")
    }

    testGenerator("org.jetbrains.kotlin.fir.dataframe.TestGeneratorKt", generateTestsInBuildDirectory = true)
}

publish {
    artifactId = "kotlin-dataframe-compiler-plugin-experimental"
}
runtimeJar()
sourcesJar()
javadocJar()

optInToExperimentalCompilerApi()
