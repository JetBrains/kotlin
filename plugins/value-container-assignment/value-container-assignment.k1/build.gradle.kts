description = "Kotlin Value Container Assignment Compiler Plugin (K1)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()
