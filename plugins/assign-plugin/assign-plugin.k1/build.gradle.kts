description = "Kotlin Assignment Compiler Plugin (K1)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":kotlin-assignment-compiler-plugin.common"))

    compileOnly(project(":analysis:kt-references"))
    compileOnly(project(":analysis:kt-references:kt-references-fe10"))
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
