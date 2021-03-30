description = "Runtime library for the Parcelize compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-android-extensions-runtime"))
    compileOnly(commonDep("com.google.android", "android"))
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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += "-Xsuppress-deprecated-jvm-target-warning"
    }
}
