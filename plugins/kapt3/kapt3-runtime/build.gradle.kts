description = "Kotlin Annotation Processing Runtime"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-stdlib"))
}

jvmTarget = "1.6"

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
sourcesJar()
javadocJar()

dist(targetName = "kotlin-annotation-processing-runtime.jar")

publish()
