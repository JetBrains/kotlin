plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Test utils for Analysis API-based Objective-C and Swift exports"

dependencies {
    compileOnly(kotlinStdlib())

    testApi(project(":analysis:analysis-api-standalone"))
    testApi(libs.junit.jupiter.api)
    testApi(libs.junit.jupiter.engine)
    testApi(libs.junit.jupiter.params)
}

sourceSets {
    "test" { projectDefault() }
}

testsJar()
