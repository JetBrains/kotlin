
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":idea"))
    implementation(project(":idea:idea-jvm"))
    implementation(project(":idea:idea-jps-common"))
    implementation(project(":idea:idea-gradle"))
    implementation(project(":idea:idea-maven"))
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) }
    compileOnly(intellijPluginDep("gradle"))
    compileOnly("org.jetbrains.intellij.deps:asm-all:9.1")
    compileOnly(intellijDep())
    compileOnly(project(":idea:kotlin-gradle-tooling"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

