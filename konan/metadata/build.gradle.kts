plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native metadata"

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:serialization"))
    compile(project(":konan:konan-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
