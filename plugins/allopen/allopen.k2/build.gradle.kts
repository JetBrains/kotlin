description = "Kotlin AllOpen Compiler Plugin (K2)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":compiler:fir:cones"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:checkers"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:fir:entrypoint"))

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
