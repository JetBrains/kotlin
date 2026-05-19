description = "Parcelize compiler plugin (CLI)"

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.common"))
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.k1"))
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.k2"))
    api(project(":plugins:parcelize:parcelize-compiler:parcelize.backend"))

    implementation(project(":compiler:container"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:language.targets.jvm"))
    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":compiler:backend"))
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
