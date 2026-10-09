description = "Kotlin NoArg Compiler Plugin (K2)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":kotlin-noarg-compiler-plugin.common"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":core:compiler.common.jvm"))

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
