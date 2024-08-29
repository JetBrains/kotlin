description = "kotlinp"

plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":kotlin-metadata"))
}

sourceSets {
    "main" { projectDefault() }
}
