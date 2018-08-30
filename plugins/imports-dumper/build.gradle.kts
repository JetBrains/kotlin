
description = "Extension for saving imports of .kt-files in JSON"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven("http://dl.bintray.com/kotlin/kotlinx")
}

val kotlinxSerializationVersion = "0.4.2"

dependencies {
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:plugin-api"))
    compileOnly("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", kotlinxSerializationVersion) { isTransitive = false }

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompile(projectTests(":compiler:tests-common"))

    embeddedComponents("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", kotlinxSerializationVersion) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
    dependsOn(":dist")
}

runtimeJar {
    fromEmbeddedComponents()
}

dist()