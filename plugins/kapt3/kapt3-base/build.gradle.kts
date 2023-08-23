plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(toolsJarApi())

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupyter.api)
    testRuntimeOnly(libs.junit.jupyter.engine)
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
