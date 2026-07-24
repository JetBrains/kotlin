description = "Parcelize compiler plugin (Backend)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(project(":plugins:parcelize:parcelize-compiler:parcelize.common"))
    implementation(project(":core:descriptors"))

    implementation(project(":compiler:backend"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:backend.jvm"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:fir:tree"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
