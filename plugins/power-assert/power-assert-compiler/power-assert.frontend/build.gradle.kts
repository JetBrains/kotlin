description = "Kotlin Power-Assert Compiler Plugin (Frontend)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:fir:plugin.api"))

    implementation(project(":kotlin-power-assert-compiler-plugin.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
