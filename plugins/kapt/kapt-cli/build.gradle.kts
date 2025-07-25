plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

dependencies {
    api(project(":compiler:cli"))

    compileOnly(intellijCore())

    testFixturesImplementation(intellijCore())
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
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

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
        dependsOn(":dist")
        val jdkHome = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
        doFirst {
            environment("JAVA_HOME", jdkHome.get())
        }
    }
}
