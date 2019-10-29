
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())

    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":idea")) { isTransitive = false }
    compileOnly(project(":idea:kotlin-gradle-tooling"))
    compileOnly(project(":idea:idea-core"))
    compileOnly(project(":idea:idea-gradle"))
    compileOnly(intellijDep())
    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java"))
    }
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()

sourcesJar()

javadocJar()

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
