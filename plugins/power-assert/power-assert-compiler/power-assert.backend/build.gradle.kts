description = "Kotlin Power-Assert Compiler Plugin (Backend)"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:cli"))

    implementation(project(":kotlin-power-assert-compiler-plugin.common"))

    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
