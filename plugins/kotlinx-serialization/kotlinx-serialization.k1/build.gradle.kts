description = "Kotlin Serialization Compiler Plugin (K1)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:compiler.common.jvm"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:ir.backend.common")) // needed for CompilationException
    compileOnly(project(":core:deserialization.common.jvm")) // needed for CompilationException

    implementation(project(":kotlinx-serialization-compiler-plugin.common"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
