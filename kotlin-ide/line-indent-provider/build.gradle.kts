plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:psi"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }
    compileOnly(intellijDep()) { includeJars("platform-api", "platform-impl", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

