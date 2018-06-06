description = "Kotlin metadata manipulation library"

plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}
