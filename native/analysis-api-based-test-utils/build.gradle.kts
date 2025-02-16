plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Test utils for Analysis API-based Objective-C and Swift exports"

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":analysis:analysis-api-standalone"))
    api(libs.junit.jupiter.api)
    api(libs.junit.jupiter.engine)
    api(libs.junit.jupiter.params)
}

sourceSets {
    "main" { projectDefault() }
}