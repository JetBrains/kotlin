description = "Parcelize compiler plugin (Backend)"

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":plugins:parcelize:parcelize-compiler:parcelize.common"))

    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:backend")) // for KotlinTypeMapper
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
