description = "kotlinp"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":kotlin-metadata"))
}

sourceSets {
    "main" { projectDefault() }
}
