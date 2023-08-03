description = "Parcelize compiler plugin (Backend)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":plugins:parcelize:parcelize-compiler:parcelize.common"))
    implementation(project(":plugins:parcelize:parcelize-compiler:parcelize.k1"))

    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
}

optInToIrSymbolInternals()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
