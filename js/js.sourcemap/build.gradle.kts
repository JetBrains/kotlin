plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":js:js.ast"))
    compileOnly(project(":js:js.parser")) // TODO remove, required for JSON AST
    compileOnly(project(":js:js.frontend")) // TODO remove
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("trove4j") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
