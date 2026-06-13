description = "Parcelize compiler plugin (Backend)"

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":plugins:parcelize:parcelize-compiler:parcelize.common"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(libs.intellij.asm)

    implementation(project(":compiler:util"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:backend")) // for KotlinTypeMapper
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToK1Deprecation()

runtimeJar()
javadocJar()
sourcesJar()
