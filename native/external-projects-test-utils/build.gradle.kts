plugins {
    kotlin("jvm")
}

description = "Infrastructure for using real-world libraries in tests"

dependencies {
    compileOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
}
