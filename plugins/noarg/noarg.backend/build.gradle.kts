description = "Kotlin NoArg Compiler Plugin (Backend)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.backend.common"))
    api(project(":core:descriptors"))
    implementation(project(":compiler:frontend.java"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
    implementation(kotlinStdlib())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
