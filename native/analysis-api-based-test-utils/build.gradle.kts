plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Test utils for Analysis API-based Objective-C and Swift exports"

dependencies {
    compileOnly(kotlinStdlib())

    testApi(project(":analysis:analysis-api-standalone"))
}

sourceSets {
    "test" { projectDefault() }
}

testsJar()