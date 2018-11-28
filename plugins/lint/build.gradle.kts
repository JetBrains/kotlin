
plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-android"))
    compile(project(":plugins:uast-kotlin"))
    compileOnly(project(":kotlin-android-extensions-runtime"))
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("android"))
}

sourceSets {
    "main" {
        java.srcDirs("android-annotations/src",
                     "lint-api/src",
                     "lint-checks/src",
                     "lint-idea/src")
    }
    "test" {}
}

