plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":compiler:config"))
}

sourceSets {
    "main" { projectDefault() }
}
