description = "Kotlin Android Extensions Runtime"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-android-extensions-parcel-runtime"))
    compile(project(":kotlin-android-extensions-synthetic-runtime"))
}

runtimeJar()
sourcesJar()
javadocJar()

dist(targetName = "android-extensions-runtime.jar")

publish()
