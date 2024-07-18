plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
