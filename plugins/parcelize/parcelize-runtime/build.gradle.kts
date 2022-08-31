description = "Runtime library for the Parcelize compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
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
