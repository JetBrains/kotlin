plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
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
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(isolated, "testData")
    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withMockJdkAnnotationsJar()

    testTask(jUnitMode = JUnitMode.JUnit5) {
        val classpathProvider = objects.newInstance<DataFramePluginClasspathProvider>()
        classpathProvider.classpath.from(dataframeRuntimeClasspath)
        jvmArgumentProviders.add(classpathProvider)
    }

    testGenerator("org.jetbrains.kotlin.fir.dataframe.TestGeneratorKt", generateTestsInBuildDirectory = true)
}

abstract class DataFramePluginClasspathProvider : CommandLineArgumentProvider {
    @get:InputFiles
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> {
        return listOf(
            "-Dkotlin.dataframe.plugin.test.classpath=${classpath.asPath}"
        )
    }
}

publish {
    artifactId = "kotlin-dataframe-compiler-plugin-experimental"
}
runtimeJar()
sourcesJar()
javadocJar()
testsJar()

optInToExperimentalCompilerApi()
