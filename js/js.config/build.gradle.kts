plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:config"))
    compileOnly(intellijCore())

    compileOnly(project(":core:metadata"))
    embedded(project(":core:metadata")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
