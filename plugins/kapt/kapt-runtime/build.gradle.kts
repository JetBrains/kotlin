description = "Kotlin Annotation Processing Runtime"

plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
