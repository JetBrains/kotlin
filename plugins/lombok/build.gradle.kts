description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

dependencies {
    embedded(project(":kotlin-lombok-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.k1")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.k2")) { isTransitive = false }
    embedded(project(":kotlin-lombok-compiler-plugin.cli")) { isTransitive = false }

    testFixturesApi(intellijCore())
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.common"))
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.k1"))
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.k2"))
    testFixturesApi(project(":kotlin-lombok-compiler-plugin.cli"))

    testFixturesApi(commonDependency("org.projectlombok:lombok"))

    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(libs.junit4)

    testRuntimeOnly(libs.guava)
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly(toolsJar())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(
        jUnitMode = JUnitMode.JUnit5,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_17_0)
    ) {
        dependsOn(":dist")
        workingDir = rootDir

        val testRuntimeClasspathFiles: FileCollection = configurations.testRuntimeClasspath.get()
        doFirst {
            testRuntimeClasspathFiles
                .find { "guava" in it.name }
                ?.absolutePath
                ?.let { systemProperty("org.jetbrains.kotlin.test.guava-location", it) }

        }
    }

    testGenerator("org.jetbrains.kotlin.lombok.TestGeneratorKt")

    withJvmStdlibAndReflect()
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()
