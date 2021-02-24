plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:deserialization"))
    api(project(":compiler:config"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
