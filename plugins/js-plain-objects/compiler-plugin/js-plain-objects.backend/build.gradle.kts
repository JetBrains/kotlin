description = "Kotlin JavaScript Plain Objects Compiler Plugin (Backend)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))

    implementation(project(":plugins:js-plain-objects:compiler-plugin:js-plain-objects.common"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
