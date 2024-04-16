description = "Fleet RhizomeDB Compiler Plugin (K2)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:backend.jvm.codegen"))
    compileOnly(project(":compiler:backend.jvm.lower"))
    compileOnly(project(":compiler:ir.tree"))

    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:fir-deserialization"))
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:fir-serialization"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:plugin-utils"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:cli-common"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
