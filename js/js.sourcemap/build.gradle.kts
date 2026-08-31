plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":js:js.ast"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
