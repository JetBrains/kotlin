description = "Kotlin JavaScript Plain Objects Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:ir.backend.common"))

    implementation(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.backend"))
    implementation(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.k2"))

    compileOnly(intellijCore())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
