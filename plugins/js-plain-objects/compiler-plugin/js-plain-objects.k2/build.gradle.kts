description = "Kotlin JavaScript Plain Objects Compiler Plugin (K2)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:plugin-utils"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:cli-common"))

    implementation(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.common"))

    compileOnly(intellijCore())

    testApi(project(":compiler:fir:plugin-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
}
