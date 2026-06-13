description = "Kotlin NoArg Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-noarg-compiler-plugin.common"))
    api(project(":kotlin-noarg-compiler-plugin.k1"))
    api(project(":kotlin-noarg-compiler-plugin.k2"))
    api(project(":kotlin-noarg-compiler-plugin.backend"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:frontend"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:language.targets.jvm"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(intellijCore())
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToK1Deprecation()

runtimeJar()
sourcesJar()
javadocJar()
