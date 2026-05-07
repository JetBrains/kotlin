description = "Lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
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

    testFixturesApi(project(":compiler:fir:plugin-utils"))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(libs.junit4)

    testRuntimeOnly(libs.guava)
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
    testRuntimeOnly(toolsJar())
    testRuntimeOnly(libs.slf4j.api)
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(
        jUnitMode = JUnitMode.JUnit5,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_17_0)
    ) {
        val testRuntimeClasspathFiles: FileCollection = configurations.testRuntimeClasspath.get()
        doFirst {
            testRuntimeClasspathFiles.forEach { file ->
                val absolutePath = file.absolutePath.let { if (File.separatorChar == '/') it else it.replace(File.separatorChar, '/') }
                when {
                    "com.google.guava/guava" in absolutePath -> {
                        systemProperty("org.jetbrains.kotlin.test.guava-location", absolutePath)
                    }
                    "org.slf4j/slf4j-api" in absolutePath -> {
                        systemProperty("org.jetbrains.kotlin.test.slf4j-location", absolutePath)
                    }
                }
            }
        }
    }

    testGenerator("org.jetbrains.kotlin.lombok.TestGeneratorKt", generateTestsInBuildDirectory = true)

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withTestJar()

    testData(project(":kotlin-lombok-compiler-plugin").isolated, "testData")
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
testsJar()
