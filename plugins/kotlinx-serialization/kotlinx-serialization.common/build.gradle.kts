description = "Kotlin Serialization Compiler Plugin (Common)"

plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":core:compiler.common"))
    compileOnly(project(":core:deserialization.common.jvm"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
