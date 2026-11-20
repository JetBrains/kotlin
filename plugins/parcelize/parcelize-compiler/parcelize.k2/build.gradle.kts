description = "Parcelize compiler plugin (Backend)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":plugins:parcelize:parcelize-compiler:parcelize.common"))
    implementation(project(":compiler:frontend.common-psi"))

    compileOnly(intellijCore())
    implementation(project(":compiler:fir:plugin.api"))
    // Required for JVM specific utility functions.
    implementation(project(":compiler:fir:fir-jvm"))
    // TODO(KT-90146): Required to access various utility functions.
    implementation(project(":compiler:fir:checkers:checkers.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
