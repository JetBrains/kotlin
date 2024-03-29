description = "Lombok compiler plugin (K2)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":kotlin-lombok-compiler-plugin.common"))
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:providers"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:java"))
    compileOnly(project(":compiler:fir:entrypoint"))

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
