plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(toolsJarApi())

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testCompileOnly(toolsJarApi())
    testFixturesCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

testsJar {}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
    }
}
