
description = "Extension for saving imports of .kt-files in JSON"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val kotlinxSerializationVersion = "0.4.2"

dependencies {
    api(project(":compiler:frontend.java"))
    api(project(":compiler:plugin-api"))
    compileOnly("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", kotlinxSerializationVersion) { isTransitive = false }

    compileOnly(intellijCore())

    testApi(projectTests(":compiler:tests-common"))

    embedded("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", kotlinxSerializationVersion) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
    dependsOn(":dist")
}

runtimeJar()
