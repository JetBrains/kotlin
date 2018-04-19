description = "Kotlin Android Extensions Runtime"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly(commonDep("com.google.android", "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
sourcesJar()
javadocJar()

dist(targetName = "android-extensions-runtime.jar")

publish()
