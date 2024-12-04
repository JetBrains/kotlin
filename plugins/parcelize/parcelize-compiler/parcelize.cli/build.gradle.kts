description = "Parcelize compiler plugin (CLI)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.common"))
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.k1"))
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.k2"))
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.backend"))

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToExperimentalCompilerApi()

runtimeJar()
javadocJar()
sourcesJar()
