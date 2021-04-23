description = "Lombok IDE plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":plugins:lombok:lombok-compiler-plugin"))
    implementation(project(":plugins:base-compiler-plugins-ide-support"))

    compileOnly(project(":idea"))
    compileOnly(project(":idea:idea-jvm"))
    compileOnly(project(":idea:idea-jps-common"))
    compileOnly(project(":idea:idea-maven"))
    compileOnly(intellijDep())
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) }
    compileOnly(intellijPluginDep("gradle"))
    compileOnly(project(":idea:kotlin-gradle-tooling"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

sourcesJar()

javadocJar()

projectTest(parallel = true)

apply(from = "$rootDir/gradle/kotlinPluginPublication.gradle.kts")
