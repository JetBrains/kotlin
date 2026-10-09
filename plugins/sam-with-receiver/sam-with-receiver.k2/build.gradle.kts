description = "Kotlin SamWithReceiver Compiler Plugin (K2)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:fir:plugin.api"))
    // TODO(KT-90146): Required to access various utility functions.
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:ir.backend.common"))

    compileOnly(intellijCore())
    runtimeOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
