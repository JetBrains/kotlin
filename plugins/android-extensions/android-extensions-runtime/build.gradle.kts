description = "Kotlin Android Extensions Runtime"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(kotlinStdlib())
    compileOnly(commonDep("com.google.android", "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

dist(targetName = "android-extensions-runtime.jar")
