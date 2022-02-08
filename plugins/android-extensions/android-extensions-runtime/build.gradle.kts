description = "Kotlin Android Extensions Runtime"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("1.6")

dependencies {
    api(kotlinStdlib())
    compileOnly(commonDependency("com.google.android", "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
