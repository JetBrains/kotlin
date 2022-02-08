description = "Runtime library for the Parcelize compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("1.6")

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-android-extensions-runtime"))
    compileOnly(commonDependency("com.google.android", "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish {
    artifactId = "kotlin-parcelize-runtime"
}

runtimeJar()
sourcesJar()
javadocJar()
