plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":js:js.ast"))
    compileOnly(project(":js:js.parser")) // TODO remove, required for JSON AST
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
