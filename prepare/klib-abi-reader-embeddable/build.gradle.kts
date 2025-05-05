description = "Kotlin KLIB abi reader embeddable"

plugins {
    kotlin("jvm")
}

dependencies {
    embedded(project(":kotlin-util-klib-abi")) {
        isTransitive = true
        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
    }
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

publish()

runtimeJar()
