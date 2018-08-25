plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native metadata"

jvmTarget = "1.6"

dependencies {
    compile(project(":core:metadata"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
