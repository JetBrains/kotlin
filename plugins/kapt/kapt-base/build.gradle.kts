plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("project-tests-convention")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(toolsJarApi())

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testCompileOnly(toolsJarApi())
    testFixturesCompileOnly(toolsJarApi())
    testFixturesApi(kotlinStdlib())
    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

testsJar {}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
    }

    withJvmStdlibAndReflect()
}
