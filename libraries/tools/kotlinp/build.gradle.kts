description = "kotlinp"

plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":kotlin-metadata"))
}

sourceSets {
    "main" { projectDefault() }
}
