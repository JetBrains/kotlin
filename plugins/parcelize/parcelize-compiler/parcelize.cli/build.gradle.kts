description = "Parcelize compiler plugin (CLI)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.common"))
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.k2"))
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.backend"))

    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":compiler:ir.backend.common"))
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
