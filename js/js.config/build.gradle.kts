plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
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
