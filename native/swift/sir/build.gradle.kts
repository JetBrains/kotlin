plugins {
    kotlin("jvm")
}

description = "Swift Intermediate Representation"

dependencies {
    compileOnly(kotlinStdlib())

    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform { }
}
