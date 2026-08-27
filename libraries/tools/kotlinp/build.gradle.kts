description = "kotlinp"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":kotlin-metadata"))
}

sourceSets {
    "main" { projectDefault() }
}
