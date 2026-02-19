description = "Kotlin NoArg Compiler Plugin (Backend)"

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:ir.backend.common"))
    api(project(":core:descriptors"))
    implementation(project(":compiler:frontend.java"))
    compileOnly(project(":kotlin-noarg-compiler-plugin.common"))
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
