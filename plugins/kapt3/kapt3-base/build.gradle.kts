plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(toolsJarApi())

    testApiJUnit5()
    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    useJUnitPlatform()
    workingDir = rootDir
}
