description = "kotlinp"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":kotlinx-metadata"))
}

sourceSets {
    "main" { projectDefault() }
}
