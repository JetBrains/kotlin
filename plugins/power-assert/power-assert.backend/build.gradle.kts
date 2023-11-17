description = "Kotlin Power-Assert Compiler Plugin (Backend)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:cli"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
