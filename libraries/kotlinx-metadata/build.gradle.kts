description = "Kotlin metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.11.0"
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}
