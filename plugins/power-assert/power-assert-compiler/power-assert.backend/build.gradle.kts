description = "Kotlin Power-Assert Compiler Plugin (Backend)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))

    // TODO(KT-90144): Needed for FirMetadataSource access.
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:fir2ir"))

    compileOnly(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm"))

    implementation(project(":kotlin-power-assert-compiler-plugin.common"))
    implementation(project(":compiler:psi:parser"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":core:descriptors"))

    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
javadocJar()
sourcesJar()
