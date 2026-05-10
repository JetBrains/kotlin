description = "Lombok compiler plugin (K2)"

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":kotlin-lombok-compiler-plugin.common"))
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:providers"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:fir-jvm"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:fir:plugin-utils"))
    compileOnly(project(":compiler:ir.tree"))
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
