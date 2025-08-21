plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Infrastructure for using real-world libraries in tests"

dependencies {
    compileOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
}