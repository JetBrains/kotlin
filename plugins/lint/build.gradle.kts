
plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":idea"))
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

