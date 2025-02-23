description = "Kotlin Power-Assert Compiler Plugin (Backend)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:cli"))

    compileOnly(intellijCore())

    implementation(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:tree"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
