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
    testRuntimeOnly(libs.log4j.over.slf4j)
    testRuntimeOnly(libs.commons.logging)
    testRuntimeOnly(libs.flogger)
    testRuntimeOnly(libs.flogger.system.backend)
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
            val librarySuffixes = listOf(
                "com.google.guava/guava",
                "org.slf4j/slf4j-api",
                "log4j-over-slf4j",
                "commons-logging/commons-logging",
                "com.google.flogger/flogger/",
                "com.google.flogger/flogger-system-backend/",
            )
            testRuntimeClasspathFiles.forEach { classPathFile ->
                val normalizedPath =
                    classPathFile.absolutePath.let { if (File.separatorChar == '/') it else it.replace(File.separatorChar, '/') }
                for (librarySuffix in librarySuffixes) {
                    if (librarySuffix in normalizedPath) {
                        systemProperty("org.jetbrains.kotlin.test.$librarySuffix", normalizedPath)
                        break
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
