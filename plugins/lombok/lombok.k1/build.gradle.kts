description = "Lombok compiler plugin (K1)"

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:resolution"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))

    implementation(project(":kotlin-lombok-compiler-plugin.common"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
