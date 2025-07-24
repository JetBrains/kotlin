plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
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

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
    workingDir = rootDir
}
