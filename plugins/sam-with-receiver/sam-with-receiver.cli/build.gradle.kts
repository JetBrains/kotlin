description = "Kotlin SamWithReceiver Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-sam-with-receiver-compiler-plugin.common"))
    api(project(":kotlin-sam-with-receiver-compiler-plugin.k1"))
    api(project(":kotlin-sam-with-receiver-compiler-plugin.k2"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:frontend"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:language.targets.jvm"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
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
