plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Test utils for Analysis API-based Objective-C and Swift exports"

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":analysis:analysis-api-standalone"))
    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.jupiter.engine)
    implementation(libs.junit.jupiter.params)
}

sourceSets {
    "main" { projectDefault() }
}
