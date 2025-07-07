plugins {
    kotlin("jvm")
}

val dataframeRuntimeClasspath by configurations.creating

dependencies {
    embedded(project(":kotlin-dataframe-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-dataframe-compiler-plugin.backend")) { isTransitive = false }
    embedded(project(":kotlin-dataframe-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-dataframe-compiler-plugin.cli")) { isTransitive = false }

    testApi(project(":kotlin-dataframe-compiler-plugin.cli"))
    testRuntimeOnly(libs.dataframe.core.dev)
    testRuntimeOnly(libs.dataframe.csv.dev)
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:fir:analysis-tests"))
    testApi(projectTests(":js:js.tests"))
    testApi(project(":compiler:fir:plugin-utils"))
    testImplementation(projectTests(":analysis:analysis-api-fir"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:low-level-api-fir"))

    dataframeRuntimeClasspath(libs.dataframe.core.dev)
    dataframeRuntimeClasspath(libs.dataframe.csv.dev)
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
    val classpathProvider = objects.newInstance<DataFramePluginClasspathProvider>()
    classpathProvider.classpath.from(dataframeRuntimeClasspath)
    jvmArgumentProviders.add(classpathProvider)
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