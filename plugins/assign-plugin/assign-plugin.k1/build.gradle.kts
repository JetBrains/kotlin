description = "Kotlin Assignment Compiler Plugin (K1)"

plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    implementation(project(":kotlin-assignment-compiler-plugin.common"))

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
