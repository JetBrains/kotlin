plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    api(project(":compiler:cli"))
    api(project(":compiler:cli-jvm"))

    compileOnly(intellijCore())

    testFixturesImplementation(intellijCore())
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

testsJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8)) {
        val jdkHome = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
        doFirst {
            environment("JAVA_HOME", jdkHome.get())
        }
    }

    testGenerator("org.jetbrains.kotlin.kapt.cli.test.TestGeneratorKt")

    withJvmStdlibAndReflect()

    // Tests scenarios invoke `kotlinc` and `kapt` scripts from dist
    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project.isolated, "testData")
}
