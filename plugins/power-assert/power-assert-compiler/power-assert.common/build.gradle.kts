description = "Kotlin Power-Assert Compiler Plugin (Common)"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":core:compiler.common"))
    compileOnly(project(":compiler:frontend.common"))
    compileOnly(project(":compiler:frontend.common-psi"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
