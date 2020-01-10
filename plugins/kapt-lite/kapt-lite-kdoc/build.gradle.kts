description = "Lightweight annotation processing support – KDoc parser"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}