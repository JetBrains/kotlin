description = "Kotlin Serialization Compiler Plugin (K1)"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":core:compiler.common.jvm"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":compiler:cli-base"))
    compileOnly(project(":compiler:config.jvm"))
    compileOnly(project(":native:native.config"))
    compileOnly(project(":compiler:ir.backend.common")) // needed for CompilationException
    compileOnly(project(":core:deserialization.common.jvm")) // needed for CompilationException

    implementation(project(":kotlinx-serialization-compiler-plugin.common"))
    implementation(project(":compiler:resolution"))
    implementation(project(":compiler:serialization"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
