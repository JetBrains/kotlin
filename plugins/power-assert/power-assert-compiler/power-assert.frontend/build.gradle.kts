description = "Kotlin Power-Assert Compiler Plugin (Frontend)"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:fir:entrypoint"))

    implementation(project(":kotlin-power-assert-compiler-plugin.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
