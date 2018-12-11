
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:serialization"))
    compile(project(":js:js.ast"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "annotations") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

