description = "Kotlin NoArg Compiler Plugin (Backend)"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:frontend"))
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
